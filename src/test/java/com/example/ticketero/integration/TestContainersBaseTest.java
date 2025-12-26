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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static io.restassured.RestAssured.given;

/**
 * Base class for integration tests using TestContainers with PostgreSQL.
 * 
 * IMPORTANT: This class requires Docker to be running.
 * If Docker is not available, use BaseIntegrationTest instead.
 * 
 * To use this class:
 * 1. Ensure Docker is running
 * 2. Extend TestContainersBaseTest instead of BaseIntegrationTest
 * 3. Run tests with: mvn test -Dtest="*TestContainersTest"
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Import(WireMockConfig.class)
public abstract class TestContainersBaseTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("ticketero_test")
            .withUsername("test")
            .withPassword("test");

    @LocalServerPort
    protected int port;

    @Autowired
    protected WireMockServer wireMockServer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("telegram.bot.token", () -> "test-token");
        registry.add("telegram.bot.chat-id", () -> "123456789");
        registry.add("telegram.api.url", () -> "http://localhost:8089");
        
        System.out.println("✅ Using TestContainers PostgreSQL for E2E tests");
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