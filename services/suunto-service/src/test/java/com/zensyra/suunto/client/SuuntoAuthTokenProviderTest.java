package com.zensyra.suunto.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.time.Instant;

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
class SuuntoAuthTokenProviderTest {

        private static ClientAndServer mockServer;

        @Inject
        SuuntoAuthTokenProvider authTokenProvider;

        @BeforeAll
        static void startMockServer() {
                mockServer = ClientAndServer.startClientAndServer(1081);
        }

        @BeforeEach
        void reset() throws Exception {
                mockServer.reset();
                resetProviderState();
        }

        @AfterAll
        static void stopMockServer() {
                if (mockServer != null) {
                        mockServer.stop();
                }
        }

        @Test
        void shouldRefreshAccessTokenSuccessfully() {

                mockServer
                                .when(
                                                HttpRequest.request()
                                                                .withMethod("POST")
                                                                .withPath("/oauth/token"))
                                .respond(
                                                HttpResponse.response()
                                                                .withStatusCode(200)
                                                                .withHeader("Content-Type", "application/json")
                                                                .withBody("""
                                                                                {
                                                                                    "access_token": "access-token-1",
                                                                                    "refresh_token": "refresh-token-2",
                                                                                    "expires_in": 3600
                                                                                }
                                                                                """));

                String accessToken = authTokenProvider.getValidAccessToken();

                assertEquals(
                                "access-token-1",
                                accessToken);

                assertEquals(
                                1,
                                mockServer.retrieveRecordedRequests(
                                                HttpRequest.request()
                                                                .withMethod("POST")
                                                                .withPath("/oauth/token")).length);
        }

        @Test
        void shouldPropagateExceptionWhenRefreshFails() {

                mockServer
                                .when(
                                                HttpRequest.request()
                                                                .withMethod("POST")
                                                                .withPath("/oauth/token"))
                                .respond(
                                                HttpResponse.response()
                                                                .withStatusCode(500));

                assertThrows(
                                RuntimeException.class,
                                () -> authTokenProvider.getValidAccessToken());

                assertEquals(
                                1,
                                mockServer.retrieveRecordedRequests(
                                                HttpRequest.request()
                                                                .withMethod("POST")
                                                                .withPath("/oauth/token")).length);
        }

        @Test
        void shouldReuseValidAccessToken() {

                mockServer
                                .when(
                                                HttpRequest.request()
                                                                .withMethod("POST")
                                                                .withPath("/oauth/token"))
                                .respond(
                                                HttpResponse.response()
                                                                .withStatusCode(200)
                                                                .withHeader("Content-Type", "application/json")
                                                                .withBody("""
                                                                                {
                                                                                    "access_token": "access-token-1",
                                                                                    "refresh_token": "refresh-token-2",
                                                                                    "expires_in": 3600
                                                                                }
                                                                                """));

                String firstToken = authTokenProvider.getValidAccessToken();

                String secondToken = authTokenProvider.getValidAccessToken();

                assertEquals(
                                "access-token-1",
                                firstToken);

                assertEquals(
                                "access-token-1",
                                secondToken);

                assertEquals(
                                1,
                                mockServer.retrieveRecordedRequests(
                                                HttpRequest.request()
                                                                .withMethod("POST")
                                                                .withPath("/oauth/token")).length);
        }

        private void resetProviderState() throws Exception {
                SuuntoAuthTokenProvider provider = io.quarkus.arc.ClientProxy.unwrap(authTokenProvider);

                Field accessToken = SuuntoAuthTokenProvider.class
                                .getDeclaredField("currentAccessToken");

                accessToken.setAccessible(true);
                accessToken.set(provider, null);

                Field tokenExpiration = SuuntoAuthTokenProvider.class
                                .getDeclaredField("tokenExpiration");

                tokenExpiration.setAccessible(true);
                tokenExpiration.set(provider, Instant.MIN);
        }

}