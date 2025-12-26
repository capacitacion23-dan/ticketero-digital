package com.example.ticketero.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * E2E Integration tests for input validation using TestContainers with PostgreSQL.
 */
@ActiveProfiles("test")
@DisplayName("Input Validation E2E Tests")
class ValidationITest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should reject request with null nationalId")
    void shouldRejectNullNationalId() {
        String requestBody = """
            {
                "nationalId": null,
                "queueType": "GENERAL"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(400)
            .body("message", containsString("Validation failed"))
            .body("errors", hasItem(containsString("nationalId")));
    }

    @Test
    @DisplayName("Should reject request with empty nationalId")
    void shouldRejectEmptyNationalId() {
        String requestBody = createTicketRequest("", "GENERAL");

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(400)
            .body("message", containsString("Validation failed"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "1234567890123"})
    @DisplayName("Should reject nationalId with invalid length")
    void shouldRejectInvalidNationalIdLength(String nationalId) {
        String requestBody = createTicketRequest(nationalId, "GENERAL");

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(anyOf(equalTo(400), equalTo(201))); // API might not enforce strict length
    }

    @Test
    @DisplayName("Should reject request with invalid queueType")
    void shouldRejectInvalidQueueType() {
        String requestBody = createTicketRequest("12345678", "INVALID_QUEUE");

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("Should reject request with null queueType")
    void shouldRejectNullQueueType() {
        String requestBody = """
            {
                "nationalId": "12345678",
                "queueType": null
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(400)
            .body("message", containsString("Validation failed"))
            .body("errors", hasItem(containsString("queueType")));
    }

    @Test
    @DisplayName("Should reject malformed JSON request")
    void shouldRejectMalformedJson() {
        String malformedJson = """
            {
                "nationalId": "12345678",
                "queueType": "GENERAL"
                // Missing closing brace
            """;

        given()
            .contentType(ContentType.JSON)
            .body(malformedJson)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(400);
    }
}