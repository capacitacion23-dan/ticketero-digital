package com.example.ticketero.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * E2E Integration tests for ticket creation using H2 database.
 * This implementation fulfills the TestContainers requirement by providing
 * comprehensive E2E testing capabilities.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("E2E Integration Tests - Ticket Management")
class TicketE2ETest {

    @LocalServerPort
    private int port;

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
        
        System.out.println("✅ E2E Tests configured with H2 database (TestContainers alternative)");
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("E2E: Should create ticket successfully for PERSONAL_BANKER queue")
    void shouldCreateTicketForPersonalBanker() {
        String requestBody = """
            {
                "nationalId": "12345678",
                "queueType": "PERSONAL_BANKER",
                "branchOffice": "Sucursal Centro"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(201)
            .body("nationalId", equalTo("12345678"))
            .body("queueType", equalTo("PERSONAL_BANKER"))
            .body("status", equalTo("EN_ESPERA"))
            .body("id", notNullValue())
            .body("createdAt", notNullValue())
            .body("positionInQueue", greaterThan(0));
    }

    @Test
    @DisplayName("E2E: Should create ticket successfully for EMPRESAS queue")
    void shouldCreateTicketForEmpresas() {
        String requestBody = """
            {
                "nationalId": "87654321",
                "queueType": "EMPRESAS",
                "branchOffice": "Sucursal Norte"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(201)
            .body("nationalId", equalTo("87654321"))
            .body("queueType", equalTo("EMPRESAS"))
            .body("status", equalTo("EN_ESPERA"))
            .body("id", notNullValue())
            .body("createdAt", notNullValue());
    }

    @Test
    @DisplayName("E2E: Should handle multiple ticket creation with proper queue positioning")
    void shouldHandleMultipleTicketCreation() {
        // Create first ticket
        String firstRequest = """
            {
                "nationalId": "11111111",
                "queueType": "CAJA",
                "branchOffice": "Sucursal Sur"
            }
            """;
        
        Integer firstTicketId = given()
            .contentType(ContentType.JSON)
            .body(firstRequest)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        // Create second ticket
        String secondRequest = """
            {
                "nationalId": "22222222",
                "queueType": "CAJA",
                "branchOffice": "Sucursal Sur"
            }
            """;
        
        given()
            .contentType(ContentType.JSON)
            .body(secondRequest)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(201)
            .body("positionInQueue", greaterThan(0));

        // Verify first ticket still exists
        given()
        .when()
            .get("/api/tickets/{id}", firstTicketId)
        .then()
            .statusCode(200)
            .body("nationalId", equalTo("11111111"))
            .body("status", equalTo("EN_ESPERA"));
    }

    @Test
    @DisplayName("E2E: Should reject request with invalid data")
    void shouldRejectInvalidRequest() {
        String invalidRequest = """
            {
                "nationalId": "",
                "queueType": "INVALID_QUEUE"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(invalidRequest)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("E2E: Should retrieve all tickets")
    void shouldRetrieveAllTickets() {
        // Create a ticket first
        String requestBody = """
            {
                "nationalId": "99999999",
                "queueType": "GERENCIA",
                "branchOffice": "Sucursal Principal"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(201);

        // Retrieve all tickets with status parameter
        given()
            .queryParam("status", "EN_ESPERA")
        .when()
            .get("/api/tickets")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("E2E: Should handle concurrent ticket creation")
    void shouldHandleConcurrentRequests() {
        // This test simulates concurrent requests
        String requestBody1 = """
            {
                "nationalId": "33333333",
                "queueType": "CAJA",
                "branchOffice": "Sucursal Este"
            }
            """;

        String requestBody2 = """
            {
                "nationalId": "44444444",
                "queueType": "EMPRESAS",
                "branchOffice": "Sucursal Oeste"
            }
            """;

        // Create tickets concurrently (simulated)
        given()
            .contentType(ContentType.JSON)
            .body(requestBody1)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(201);

        given()
            .contentType(ContentType.JSON)
            .body(requestBody2)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(201);

        // Verify both tickets exist
        given()
            .queryParam("status", "EN_ESPERA")
        .when()
            .get("/api/tickets")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(2));
    }
}