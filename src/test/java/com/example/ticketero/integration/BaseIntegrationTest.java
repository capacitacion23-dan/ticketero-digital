package com.example.ticketero.integration;

import com.example.ticketero.config.WireMockConfig;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static io.restassured.RestAssured.given;

/**
 * Base class for integration tests.
 * Uses H2 in-memory database for testing.
 * 
 * Note: This implementation provides E2E testing capabilities.
 * For full TestContainers with PostgreSQL, Docker environment is required.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(WireMockConfig.class)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected WireMockServer wireMockServer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Use H2 for testing (TestContainers alternative when Docker not available)
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("telegram.bot.token", () -> "test-token");
        registry.add("telegram.bot.chat-id", () -> "123456789");
        registry.add("telegram.api.url", () -> "http://localhost:8089");
        
        System.out.println("⚠️  Using H2 in-memory database for E2E tests (TestContainers requires Docker)");
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        WireMockConfig.resetMocks(wireMockServer);
    }

    protected String createTicketRequest(String nationalId, String queueType) {
        return """
            {
                "nationalId": "%s",
                "queueType": "%s"
            }
            """.formatted(nationalId, queueType);
    }

    protected void assertTicketCreated(String response, String expectedNationalId, String expectedQueueType) {
        given()
            .contentType(ContentType.JSON)
            .body(response)
        .when()
            .then()
            .statusCode(201)
            .body("nationalId", org.hamcrest.Matchers.equalTo(expectedNationalId))
            .body("queueType", org.hamcrest.Matchers.equalTo(expectedQueueType))
            .body("status", org.hamcrest.Matchers.equalTo("EN_ESPERA"))
            .body("id", org.hamcrest.Matchers.notNullValue())
            .body("createdAt", org.hamcrest.Matchers.notNullValue());
    }
}