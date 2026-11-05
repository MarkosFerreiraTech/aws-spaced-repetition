package com.spacedrep.integration;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Variante para quem nao tem Docker: usa um Postgres ja existente.
 *
 * <pre>
 * export TEST_DB_URL=jdbc:postgresql://localhost:5432/spacedrep
 * export TEST_DB_USER=spacedrep
 * export TEST_DB_PASSWORD=spacedrep
 * mvn verify
 * </pre>
 *
 * Sem as variaveis, o teste e automaticamente ignorado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
class ReviewFlowLocalIT extends ReviewFlowContract {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("TEST_DB_USER"));
        registry.add("spring.datasource.password", () -> System.getenv("TEST_DB_PASSWORD"));
        registry.add("app.gemini.base-url",
                () -> "http://localhost:" + ReviewFlowContract.GEMINI.port());
    }
}
