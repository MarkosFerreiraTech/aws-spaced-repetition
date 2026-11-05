package com.spacedrep.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.spacedrep.domain.exception.InvalidLlmResponseException;
import com.spacedrep.domain.exception.LlmUnavailableException;
import com.spacedrep.domain.model.DraftQuestion;
import com.spacedrep.infrastructure.llm.GeminiClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testa o cliente do Gemini sem chamar a API real: zero custo de cota e sem flakiness
 * de rede. Cobre o que mais quebra em producao: resposta malformada, 5xx e 429.
 */
@DisplayName("GeminiClient")
class GeminiClientTest {

    private static final String MODEL_PATH = "/v1beta/models/gemini-2.0-flash:generateContent";

    private WireMockServer server;
    private GeminiClient client;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        client = new GeminiClient(RestClient.builder(), "chave-de-teste",
                "http://localhost:" + server.port(), "gemini-2.0-flash", 5);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    /** Monta o envelope do Gemini com o texto devolvido, escapando via Jackson. */
    private static String envelope(String innerJson) throws Exception {
        String quoted = new ObjectMapper().writeValueAsString(innerJson);
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" + quoted + "}]}}]}";
    }

    private static final String TWO_QUESTIONS = """
            {"questions":[
              {"stem":"Pergunta 1","topic":"IAM","multiSelect":false,"explanation":"exp1",
               "options":[{"position":1,"text":"A","correct":true},{"position":2,"text":"B","correct":false}]},
              {"stem":"Pergunta 2","topic":"VPC","multiSelect":true,"explanation":null,
               "options":[{"position":1,"text":"A","correct":true},{"position":2,"text":"B","correct":true}]}
            ]}""";

    @Test
    @DisplayName("resposta valida vira questoes estruturadas")
    void parsesValidResponse() throws Exception {
        server.stubFor(post(urlPathEqualTo(MODEL_PATH))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(envelope(TWO_QUESTIONS))));

        List<DraftQuestion> questions = client.parseQuestions("Simulado", "texto bruto");

        assertThat(questions).hasSize(2);
        assertThat(questions.get(0).stem()).isEqualTo("Pergunta 1");
        assertThat(questions.get(1).multiSelect()).isTrue();
        assertThat(questions.get(1).correctOptions()).hasSize(2);
    }

    @Test
    @DisplayName("a API key vai no header, nunca na URL")
    void apiKeyIsSentAsHeader() throws Exception {
        server.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(envelope(TWO_QUESTIONS))));

        client.parseQuestions("Simulado", "texto");

        server.verify(postRequestedFor(urlPathEqualTo(MODEL_PATH))
                .withHeader("x-goog-api-key", com.github.tomakehurst.wiremock.client.WireMock.equalTo("chave-de-teste")));
        assertThat(server.findAll(postRequestedFor(anyUrl())).get(0).getUrl())
                .as("a URL nao pode conter a chave")
                .doesNotContain("chave-de-teste");
    }

    @Test
    @DisplayName("markdown em volta do JSON e tolerado")
    void toleratesCodeFence() throws Exception {
        String fenced = "```json\n" + TWO_QUESTIONS + "\n```";
        server.stubFor(post(urlPathEqualTo(MODEL_PATH))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(envelope(fenced))));

        assertThat(client.parseQuestions("Simulado", "texto")).hasSize(2);
    }

    @Test
    @DisplayName("JSON invalido vira 422, nao 500")
    void invalidJsonIsUnprocessable() throws Exception {
        server.stubFor(post(urlPathEqualTo(MODEL_PATH))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(envelope("isto nao e json"))));

        assertThatThrownBy(() -> client.parseQuestions("Simulado", "texto"))
                .isInstanceOf(InvalidLlmResponseException.class);
    }

    @Test
    @DisplayName("5xx persistente vira indisponibilidade apos retry")
    void persistentServerErrorBecomesUnavailable() {
        server.stubFor(post(urlPathEqualTo(MODEL_PATH)).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> client.parseQuestions("Simulado", "texto"))
                .isInstanceOf(LlmUnavailableException.class);
        assertThat(server.findAll(postRequestedFor(anyUrl())))
                .as("deve tentar duas vezes")
                .hasSize(2);
    }

    @Test
    @DisplayName("429 na primeira tentativa e recuperado na segunda")
    void retriesAfterRateLimit() throws Exception {
        server.stubFor(post(urlPathEqualTo(MODEL_PATH)).inScenario("cota")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(429))
                .willSetStateTo("liberou"));
        server.stubFor(post(urlPathEqualTo(MODEL_PATH)).inScenario("cota")
                .whenScenarioStateIs("liberou")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(envelope(TWO_QUESTIONS))));

        assertThat(client.parseQuestions("Simulado", "texto")).hasSize(2);
    }

    @Test
    @DisplayName("erro 4xx de requisicao nao e repetido")
    void clientErrorIsNotRetried() {
        server.stubFor(post(urlPathEqualTo(MODEL_PATH)).willReturn(aResponse().withStatus(400)));

        assertThatThrownBy(() -> client.parseQuestions("Simulado", "texto"))
                .isInstanceOf(InvalidLlmResponseException.class);
        assertThat(server.findAll(postRequestedFor(anyUrl()))).hasSize(1);
    }

    @Test
    @DisplayName("sem API key configurada, falha antes de sair para a rede")
    void missingApiKeyFailsFast() {
        GeminiClient semChave = new GeminiClient(RestClient.builder(), "",
                "http://localhost:" + server.port(), "gemini-2.0-flash", 5);

        assertThatThrownBy(() -> semChave.parseQuestions("Simulado", "texto"))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("GEMINI_API_KEY");
        assertThat(server.findAll(postRequestedFor(anyUrl()))).isEmpty();
    }
}
