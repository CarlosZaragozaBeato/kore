package com.zensyra.suunto.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

@QuarkusTest
class SuuntoApiClientRetryTest {

    private static ClientAndServer mockServer;

    @Inject
    @RestClient
    SuuntoApiClient suuntoApiClient;

    @BeforeAll
    static void startMockServer() {
        mockServer = ClientAndServer.startClientAndServer(1080);
    }

    @BeforeEach
    void resetMockServer() {
        mockServer.reset();
    }

    @AfterAll
    static void stopMockServer() {
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @Test
    void shouldRetryThreeTimesOn429() {

        mockServer
            .when(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/v3/workouts")
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(429)
            );

        assertThrows(
            SuuntoRetryableException.class,
            () -> suuntoApiClient.listWorkouts(
                "Bearer test",
                "test",
                0,
                0,
                50,
                0,
                true
            )
        );

        assertEquals(
            4,
            mockServer.retrieveRecordedRequests(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/v3/workouts")
            ).length
        );
    }

    @Test
    void shouldRetryThreeTimesOn500() {

        mockServer
            .when(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/v3/workouts")
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(500)
            );

        assertThrows(
            SuuntoRetryableException.class,
            () -> suuntoApiClient.listWorkouts(
                "Bearer test",
                "test",
                0,
                0,
                50,
                0,
                true
            )
        );

        assertEquals(
            4,
            mockServer.retrieveRecordedRequests(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/v3/workouts")
            ).length
        );
    }

    @Test
    void shouldNotRetryOn401() {

        mockServer
            .when(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/v3/workouts")
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(401)
            );

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> suuntoApiClient.listWorkouts(
                "Bearer test",
                "test",
                0,
                0,
                50,
                0,
                true
            )
        );

        assertFalse(exception instanceof SuuntoRetryableException);

        assertEquals(
            1,
            mockServer.retrieveRecordedRequests(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/v3/workouts")
            ).length
        );
    }
}