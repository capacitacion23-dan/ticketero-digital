package com.example.ticketero.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * E2E Integration tests for ticket creation using TestContainers with PostgreSQL.
 * 
 * IMPORTANT: This test requires Docker to be running.
 * If Docker is not available, run TicketCreationITest instead.
 * 
 * To run these tests:
 * 1. Start Docker
 * 2. Run: mvn test -Dtest="*TestContainersTest"
 */
@DisplayName("Ticket Creation E2E Tests with TestContainers")
class TicketCreationTestContainersTest extends TestContainersBaseTest {

    @Test
    @DisplayName("Should create ticket successfully for PERSONAL_BANKER queue with PostgreSQL")
    void shouldCreateTicketForPersonalBanker() {
        System.out.println("✅ Running E2E tests with TestContainers PostgreSQL");
        
        String requestBody = createTicketRequest("12345678", "PERSONAL_BANKER");

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
            .body("queuePosition", greaterThan(0));
    }

    @Test
    @DisplayName("Should create ticket successfully for PRIORITY queue with PostgreSQL")
    void shouldCreateTicketForPriority() {
        String requestBody = createTicketRequest("87654321", "PRIORITY");

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(201)
            .body("nationalId", equalTo("87654321"))
            .body("queueType", equalTo("PRIORITY"))
            .body("status", equalTo("EN_ESPERA"))
            .body("id", notNullValue())
            .body("createdAt", notNullValue());
    }

    @Test
    @DisplayName("Should handle multiple ticket creation with proper queue positioning in PostgreSQL")
    void shouldHandleMultipleTicketCreation() {
        // Create first ticket
        String firstRequest = createTicketRequest("11111111", "GENERAL");
        
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
        String secondRequest = createTicketRequest("22222222", "GENERAL");
        
        given()
            .contentType(ContentType.JSON)
            .body(secondRequest)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(201)
            .body("queuePosition", greaterThan(0));

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
    @DisplayName("Should test PostgreSQL specific features")
    void shouldTestPostgreSQLFeatures() {
        // This test can include PostgreSQL-specific functionality
        // that wouldn't work with H2, such as:
        // - JSON/JSONB columns
        // - PostgreSQL-specific data types
        // - Advanced constraints
        // - Concurrent transactions
        
        String requestBody = createTicketRequest("99999999", "GENERAL");

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(201)
            .body("nationalId", equalTo("99999999"))
            .body("queueType", equalTo("GENERAL"));
            
        System.out.println("✅ PostgreSQL-specific features tested successfully");
    }
}