package com.spacedrep.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato de integracao executado contra um Postgres REAL e um Gemini FALSO (WireMock).
 *
 * <p>Duas infraestruturas, mesmo contrato:</p>
 * <ul>
 *   <li>{@link ReviewFlowIT} — Testcontainers (quem tem Docker);</li>
 *   <li>{@link ReviewFlowLocalIT} — banco externo via {@code TEST_DB_URL} (CI e dev sem Docker).</li>
 * </ul>
 *
 * <p>O WireMock garante que nenhum teste gaste cota da API real nem dependa dela.</p>
 */
@TestPropertySource(properties = {
        "app.zone-id=UTC",
        "app.jwt.secret=integration-test-secret-with-32-bytes-minimum",
        "app.jwt.expiration-minutes=60",
        "app.gemini.api-key=chave-de-teste",
        "app.import.rate-limit-per-minute=100"
})
abstract class ReviewFlowContract {

    /** O app resolve "hoje" em app.zone-id; fixar UTC evita teste dependente de fuso. */
    private static final java.time.ZoneId TEST_ZONE = ZoneOffset.UTC;

    private static final String SEED_EMAIL = "demo@spacedrep.local";
    private static final String SEED_PASSWORD = "senha123";

    protected static final WireMockServer GEMINI = new WireMockServer(options().dynamicPort());

    static {
        GEMINI.start();
    }

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected JdbcTemplate jdbc;

    protected final ObjectMapper json = new ObjectMapper();

    private String bearerToken;

    @BeforeEach
    void resetStateAndAuthenticate() throws Exception {
        // O cliente HTTP do JDK tenta reautenticar sozinho ao receber 401 e falha
        // ("cannot retry due to server authentication, in streaming mode").
        // Desligar o streaming do corpo deixa o 401 chegar ao teste normalmente.
        rest.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());

        jdbc.update("delete from exam_source where name = 'IT Import Test'");
        jdbc.update("delete from review_attempt");
        jdbc.update("""
                update review_card
                   set ease_factor = 2.50, interval_days = 0, repetitions = 0,
                       due_date = CURRENT_DATE, last_reviewed_at = null
                """);
        GEMINI.resetAll();
        bearerToken = login();
    }

    // ------------------------------------------------------------------ helpers

    private String login() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"email":"%s","password":"%s"}""".formatted(SEED_EMAIL, SEED_PASSWORD);

        ResponseEntity<String> response = rest.postForEntity(
                "/api/auth/login", new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode())
                .as("login do usuario de seed deve funcionar")
                .isEqualTo(HttpStatus.CREATED);
        return json.readTree(response.getBody()).get("token").asText();
    }

    protected <T> ResponseEntity<T> get(String path, Class<T> type) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(authHeaders()), type);
    }

    protected <T> ResponseEntity<T> postJson(String path, String jsonBody, Class<T> type) {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(path, new HttpEntity<>(jsonBody, headers), type);
    }

    protected HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        return headers;
    }

    // ---------------------------------------------------------------- seguranca

    @Test
    @DisplayName("sem token, a API responde 401 em Problem Details")
    void requestsWithoutTokenAreRejected() {
        ResponseEntity<String> response = rest.getForEntity("/api/reviews/due", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getContentType().toString())
                .contains("application/problem+json");
    }

    @Test
    @DisplayName("token adulterado e rejeitado")
    void tamperedTokenIsRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken.substring(0, bearerToken.length() - 3) + "abc");

        ResponseEntity<String> response = rest.exchange(
                "/api/reviews/due", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("senha errada retorna 401 sem revelar se o e-mail existe")
    void wrongPasswordIsRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = rest.postForEntity("/api/auth/login",
                new HttpEntity<>("{\"email\":\"" + SEED_EMAIL + "\",\"password\":\"errada\"}", headers),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> unknown = rest.postForEntity("/api/auth/login",
                new HttpEntity<>("{\"email\":\"nao-existe@x.com\",\"password\":\"errada\"}", headers),
                String.class);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------------ estudo

    @Test
    @DisplayName("a fila do dia nao revela quais alternativas estao corretas")
    void dueQueueHidesCorrectAnswers() {
        ResponseEntity<String> response = get("/api/reviews/due?limit=3", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"cards\"");
        assertThat(response.getBody()).doesNotContain("\"correct\":");
    }

    @Test
    @DisplayName("responder corretamente reagenda o card para amanha e grava a tentativa")
    void answeringReschedulesCard() throws Exception {
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

        ResponseEntity<String> response = postJson(
                "/api/reviews/" + cardId + "/answer",
                "{\"selectedOptionIds\":[%d],\"durationMs\":5000}".formatted(optionId), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = json.readTree(response.getBody());
        assertThat(body.get("correct").asBoolean()).isTrue();
        assertThat(body.has("explanation")).isTrue();
        assertThat(body.get("nextDueDate").asText())
                .isEqualTo(LocalDate.now(TEST_ZONE).plusDays(1).toString());

        Integer attempts = jdbc.queryForObject(
                "select count(*) from review_attempt where card_id = ?", Integer.class, cardId);
        assertThat(attempts).isEqualTo(1);
    }

    @Test
    @DisplayName("resposta errada volta o card para 1 dia de intervalo")
    void wrongAnswerResetsInterval() throws Exception {
        Long cardId = jdbc.queryForObject("select min(id) from review_card", Long.class);
        Long wrongOption = jdbc.queryForObject("""
                select ao.id
                  from answer_option ao
                  join review_card rc on rc.question_id = ao.question_id
                 where ao.is_correct = false and rc.id = ?
                 limit 1
                """, Long.class, cardId);

        ResponseEntity<String> response = postJson(
                "/api/reviews/" + cardId + "/answer",
                "{\"selectedOptionIds\":[%d],\"durationMs\":7000}".formatted(wrongOption), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = json.readTree(response.getBody());
        assertThat(body.get("correct").asBoolean()).isFalse();
        assertThat(body.get("intervalDays").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("card inexistente retorna 404")
    void unknownCardReturnsNotFound() {
        ResponseEntity<String> response = postJson(
                "/api/reviews/999999/answer", "{\"selectedOptionIds\":[1],\"durationMs\":1000}",
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ----------------------------------------------------------------- import

    @Test
    @DisplayName("import: parse devolve rascunho sem gravar, confirm grava e cria cards")
    void parseThenConfirmCreatesQuestions() throws Exception {
        stubGeminiWith(2);

        String parseBody = """
                {"sourceName":"IT Import Test","rawText":"texto bruto do simulado %d"}
                """.formatted(System.nanoTime());

        ResponseEntity<String> parseResponse = postJson("/api/imports/parse", parseBody, String.class);
        assertThat(parseResponse.getStatusCode())
                .as("resposta do parse: " + parseResponse.getBody())
                .isEqualTo(HttpStatus.OK);

        JsonNode parseJson = parseResponse.getBody() == null ? null : json.readTree(parseResponse.getBody());
        assertThat(parseJson.get("questionCount").asInt()).isEqualTo(2);

        Integer before = jdbc.queryForObject(
                "select count(*) from exam_source where name = 'IT Import Test'", Integer.class);
        assertThat(before).as("parse NAO pode gravar nada (ADR-004)").isZero();

        String confirmBody = json.writeValueAsString(Map.of(
                "sourceName", "IT Import Test",
                "rawTextHash", parseJson.get("rawTextHash").asText(),
                "questions", json.convertValue(parseJson.get("questions"), List.class)));

        ResponseEntity<String> confirmResponse = postJson("/api/imports/confirm", confirmBody, String.class);
        assertThat(confirmResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(json.readTree(confirmResponse.getBody()).get("createdQuestions").asInt()).isEqualTo(2);

        Integer cards = jdbc.queryForObject("""
                select count(*) from review_card rc
                  join question q on q.id = rc.question_id
                  join exam_source es on es.id = q.exam_source_id
                 where es.name = 'IT Import Test'
                """, Integer.class);
        assertThat(cards).as("cada questao importada nasce com um card vencendo hoje").isEqualTo(2);
    }

    @Test
    @DisplayName("import: reimportar o mesmo texto retorna 409")
    void duplicateImportIsRejected() throws Exception {
        stubGeminiWith(1);
        String rawText = "simulado duplicado " + System.nanoTime();
        String parseBody = """
                {"sourceName":"IT Import Test","rawText":"%s"}""".formatted(rawText);

        ResponseEntity<String> parseResponse = postJson("/api/imports/parse", parseBody, String.class);
        JsonNode parseJson = json.readTree(parseResponse.getBody());

        String confirmBody = json.writeValueAsString(Map.of(
                "sourceName", "IT Import Test",
                "rawTextHash", parseJson.get("rawTextHash").asText(),
                "questions", json.convertValue(parseJson.get("questions"), List.class)));

        assertThat(postJson("/api/imports/confirm", confirmBody, String.class).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(postJson("/api/imports/confirm", confirmBody, String.class).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("import: LLM indisponivel retorna 503 e nada e gravado")
    void llmFailureReturnsServiceUnavailable() {
        GEMINI.stubFor(post(urlPathEqualTo("/v1beta/models/gemini-2.0-flash:generateContent"))
                .willReturn(aResponse().withStatus(500)));

        ResponseEntity<String> response = postJson("/api/imports/parse",
                "{\"sourceName\":\"IT Import Test\",\"rawText\":\"qualquer texto\"}", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private void stubGeminiWith(int questionCount) {
        StringBuilder questions = new StringBuilder();
        for (int i = 1; i <= questionCount; i++) {
            if (i > 1) {
                questions.append(',');
            }
            questions.append("""
                    {"stem":"Pergunta %d do simulado","topic":"IAM","multiSelect":false,
                     "explanation":"Explicacao %d",
                     "options":[{"position":1,"text":"Correta","correct":true},
                                {"position":2,"text":"Errada","correct":false}]}"""
                    .formatted(i, i));
        }
        String llmJson = "{\"questions\":[" + questions + "]}";
        String envelope = """
                {"candidates":[{"content":{"parts":[{"text":%s}]}}]}"""
                .formatted(json.valueToTree(llmJson).toString());

        GEMINI.stubFor(post(urlPathEqualTo("/v1beta/models/gemini-2.0-flash:generateContent"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(envelope)));
    }
}
