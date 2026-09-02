package com.zensyra.ccollector.core.client.suunto;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Cliente de la Cloud API de Suunto. Requiere el token OAuth y además la
 * subscription key de Azure API Management.
 * Base URL: {@code cloudapi.suunto.com} (config quarkus.rest-client.suunto-api.url).
 */
@RegisterRestClient(configKey = "suunto-api")
public interface SuuntoApiClient {

    @GET
    @Path("/v2/workouts")
    @Produces(MediaType.APPLICATION_JSON)
    SuuntoWorkoutsResponse listWorkouts(
            @HeaderParam("Authorization") String authorization,
            @HeaderParam("Ocp-Apim-Subscription-Key") String subscriptionKey);
}
