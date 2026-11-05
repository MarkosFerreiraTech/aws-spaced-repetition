package com.spacedrep.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato de integracao executado contra um Postgres REAL.
 *
 * <p>Roda em duas variantes, com o mesmo conjunto de testes:</p>
 * <ul>
 *   <li>{@link ReviewFlowIT} — Testcontainers (CI, exige Docker);</li>
 *   <li>{@link ReviewFlowLocalIT} — banco externo via {@code TEST_DB_URL}
 *       (dev sem Docker).</li>
 * </ul>
 *
 * <p>Aqui ficam verificados de fato: migrations Flyway, mapeamento JPA com
 * {@code ddl-auto=validate}, serializacao do contrato HTTP e efeito no banco.</p>
 */
@TestPropertySource(properties = "app.zone-id=UTC")
abstract class ReviewFlowContract {

    /**
     * O app resolve "hoje" no fuso de {@code app.zone-id} (padrao America/Sao_Paulo).
     * O teste fixa o fuso em UTC para nao depender do fuso do runner: sem isso, um
     * build rodando as 01:14 UTC comparava com o dia seguinte em Sao Paulo e falhava.
     * Bug encontrado no CI, nao no ambiente local.
     */
    private static final java.time.ZoneId TEST_ZONE = ZoneOffset.UTC;

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected JdbcTemplate jdbc;

    /** POST com Content-Type correto: String pura iria como text/plain e tomaria 415. */
    protected <T> ResponseEntity<T> postJson(String path, String json, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(path, new HttpEntity<>(json, headers), responseType);
    }

    /** Torna o teste deterministico e reexecutavel (o banco local persiste entre execucoes). */
    @BeforeEach
    void resetState() {
        jdbc.update("delete from review_attempt");
        jdbc.update("""
                update review_card
                   set ease_factor = 2.50, interval_days = 0, repetitions = 0,
                       due_date = CURRENT_DATE, last_reviewed_at = null
                """);
    }

    @Test
    @DisplayName("a fila do dia nao revela quais alternativas estao corretas")
    void dueQueueHidesCorrectAnswers() {
        ResponseEntity<String> response = rest.getForEntity("/api/reviews/due?limit=3", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"cards\"");
        assertThat(response.getBody()).doesNotContain("\"correct\":");
    }

    @Test
    @DisplayName("responder corretamente reagenda o card para amanha e grava a tentativa")
    void answeringReschedulesCard() {
        List<Map<String, Object>> correctOptions = jdbc.queryForList("""
                select ao.id, ao.question_id
                  from answer_option ao
                  join review_card rc on rc.question_id = ao.question_id
                 where ao.is_correct = true
                   and rc.id = (select min(id) from review_card)
                """);
        assertThat(correctOptions).isNotEmpty();

        long optionId = ((Number) correctOptions.get(0).get("id")).longValue();
        long questionId = ((Number) correctOptions.get(0).get("question_id")).longValue();
        Long cardId = jdbc.queryForObject(
                "select id from review_card where question_id = ? order by id limit 1",
                Long.class, questionId);

        String body = "{\"selectedOptionIds\":[%d],\"durationMs\":5000}".formatted(optionId);
        ResponseEntity<Map> response = postJson("/api/reviews/" + cardId + "/answer", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("correct", true);
        assertThat(response.getBody()).containsKey("explanation");
        assertThat(response.getBody().get("nextDueDate"))
                .isEqualTo(LocalDate.now(TEST_ZONE).plusDays(1).toString());

        Integer attempts = jdbc.queryForObject(
                "select count(*) from review_attempt where card_id = ?", Integer.class, cardId);
        assertThat(attempts).isEqualTo(1);

        Integer repetitions = jdbc.queryForObject(
                "select repetitions from review_card where id = ?", Integer.class, cardId);
        assertThat(repetitions).isEqualTo(1);
    }

    @Test
    @DisplayName("resposta errada volta o card para 1 dia de intervalo")
    void wrongAnswerResetsInterval() {
        Long cardId = jdbc.queryForObject("select min(id) from review_card", Long.class);
        Long wrongOption = jdbc.queryForObject("""
                select ao.id
                  from answer_option ao
                  join review_card rc on rc.question_id = ao.question_id
                 where ao.is_correct = false and rc.id = ?
                 limit 1
                """, Long.class, cardId);

        ResponseEntity<Map> response = postJson(
                "/api/reviews/" + cardId + "/answer",
                "{\"selectedOptionIds\":[%d],\"durationMs\":7000}".formatted(wrongOption), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("correct", false);
        assertThat(response.getBody()).containsEntry("intervalDays", 1);
        assertThat(response.getBody().get("nextDueDate"))
                .isEqualTo(LocalDate.now(TEST_ZONE).plusDays(1).toString());
    }

    @Test
    @DisplayName("payload invalido retorna 400 no formato Problem Details")
    void invalidPayloadReturnsProblemDetail() {
        ResponseEntity<String> response = postJson(
                "/api/reviews/1/answer", "{\"selectedOptionIds\":[],\"durationMs\":-1}", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType().toString())
                .contains("application/problem+json");
    }

    @Test
    @DisplayName("card inexistente retorna 404")
    void unknownCardReturnsNotFound() {
        ResponseEntity<String> response = postJson(
                "/api/reviews/999999/answer", "{\"selectedOptionIds\":[1],\"durationMs\":1000}", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
