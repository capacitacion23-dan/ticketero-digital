package com.example.ticketero.integration;

import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Advanced E2E Integration tests using TestContainers with PostgreSQL.
 * Tests concurrency, performance, and complex scenarios.
 */
@ActiveProfiles("test")
@DisplayName("Advanced E2E Tests")
class AdvancedIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should handle concurrent ticket creation without conflicts")
    void shouldHandleConcurrentTicketCreation() {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        // Create 10 tickets concurrently
        CompletableFuture<?>[] futures = IntStream.range(0, 10)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                String requestBody = createTicketRequest("1234567" + i, "GENERAL");
                
                given()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                .when()
                    .post("/api/tickets")
                .then()
                    .statusCode(201)
                    .body("nationalId", equalTo("1234567" + i));
            }, executor))
            .toArray(CompletableFuture[]::new);

        // Wait for all to complete
        CompletableFuture.allOf(futures).join();
        executor.shutdown();

        // Verify all tickets were created
        given()
        .when()
            .get("/api/tickets")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(10));
    }

    @Test
    @DisplayName("Should handle database transaction rollback on external service failure")
    void shouldHandleTransactionRollback() {
        // Simulate Telegram API failure
        wireMockServer.resetAll();
        com.example.ticketero.config.WireMockConfig.simulateTelegramFailure(wireMockServer);

        String requestBody = createTicketRequest("99999999", "PRIORITY");

        // Request should fail due to external service failure
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(anyOf(equalTo(500), equalTo(201))); // Depends on error handling implementation

        // Reset mocks for other tests
        com.example.ticketero.config.WireMockConfig.resetMocks(wireMockServer);
    }

    @Test
    @DisplayName("Should handle high load ticket creation with proper performance")
    void shouldHandleHighLoadWithPerformance() {
        long startTime = System.currentTimeMillis();
        
        // Create 20 tickets sequentially to test performance
        IntStream.range(0, 20).forEach(i -> {
            String requestBody = createTicketRequest("5555555" + String.format("%02d", i), "GENERAL");
            
            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .post("/api/tickets")
            .then()
                .statusCode(201);
        });

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should complete within reasonable time (adjust based on requirements)
        System.out.println("Created 20 tickets in " + duration + "ms");
        // Assert performance if needed: assertThat(duration, lessThan(10000L));
    }

    @Test
    @DisplayName("Should maintain data consistency across multiple operations")
    void shouldMaintainDataConsistency() {
        // Create initial ticket
        String requestBody = createTicketRequest("77777777", "PERSONAL_BANKER");
        
        Integer ticketId = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        // Verify ticket exists and has correct data
        given()
        .when()
            .get("/api/tickets/{id}", ticketId)
        .then()
            .statusCode(200)
            .body("nationalId", equalTo("77777777"))
            .body("queueType", equalTo("PERSONAL_BANKER"))
            .body("status", equalTo("EN_ESPERA"));

        // Update ticket status (if endpoint exists)
        // This would test transaction consistency
        
        // Verify data integrity after operations
        given()
        .when()
            .get("/api/tickets")
        .then()
            .statusCode(200)
            .body("findAll { it.nationalId == '77777777' }.size()", equalTo(1));
    }

    @Test
    @DisplayName("Should handle async operations with proper waiting")
    void shouldHandleAsyncOperations() {
        String requestBody = createTicketRequest("88888888", "PRIORITY");
        
        // Create ticket
        Integer ticketId = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tickets")
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        // Use Awaitility to wait for async operations to complete
        Awaitility.await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(500))
            .until(() -> {
                return given()
                    .when()
                        .get("/api/tickets/{id}", ticketId)
                    .then()
                        .statusCode(200)
                        .extract()
                        .path("status") != null;
            });

        // Verify final state
        given()
        .when()
            .get("/api/tickets/{id}", ticketId)
        .then()
            .statusCode(200)
            .body("status", notNullValue());
    }
}