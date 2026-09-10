package com.zensyra.suunto.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.zensyra.suunto.dto.SuuntoTokenResponse;

/**
 * Cliente del endpoint OAuth de Suunto. Solo usamos el grant de refresh:
 * el usuario aporta client-id/secret/refresh-token ya obtenidos.
 * Base URL: {@code cloudapi-oauth.suunto.com} (config quarkus.rest-client.suunto-oauth.url).
 */
@RegisterRestClient(configKey = "suunto-oauth")
public interface SuuntoOAuthClient {

    @POST
    @Path("/oauth/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    SuuntoTokenResponse refresh(
            @HeaderParam("Authorization") String basicAuth,
            @jakarta.ws.rs.FormParam("grant_type") String grantType,
            @jakarta.ws.rs.FormParam("refresh_token") String refreshToken);
}
