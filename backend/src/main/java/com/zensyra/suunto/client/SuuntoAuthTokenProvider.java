package com.zensyra.suunto.client;

import com.zensyra.suunto.dto.SuuntoTokenResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@ApplicationScoped
public class SuuntoAuthTokenProvider {

    @ConfigProperty(name = "suunto.client-id")
    String clientId;

    @ConfigProperty(name = "suunto.client-secret")
    String clientSecret;

    @ConfigProperty(name = "suunto.initial-refresh-token")
    String currentRefreshToken;

    @Inject
    @RestClient
    SuuntoOAuthClient oAuthClient;

    private String currentAccessToken;
    private Instant tokenExpiration = Instant.MIN;

    public synchronized String getValidAccessToken() {
        if (currentAccessToken == null || Instant.now().isAfter(tokenExpiration.minusSeconds(60))) {
            refreshToken();
        }
        return currentAccessToken;
    }

    private void refreshToken() {
        String credentials = clientId + ":" + clientSecret;
        String basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        SuuntoTokenResponse response = oAuthClient.refresh(
                basicAuthHeader,
                "refresh_token",
                currentRefreshToken
        );

        this.currentAccessToken = response.accessToken();
        this.currentRefreshToken = response.refreshToken();
        this.tokenExpiration = Instant.now().plusSeconds(response.expiresIn());
    }
}