package com.zensyra.suunto.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "suunto-api")
public interface SuuntoApiClient {

    @GET
    @Path("/v2/workouts")
    @Produces(MediaType.APPLICATION_JSON)
    String listWorkouts(
            @HeaderParam("Authorization") String authorization,
            @HeaderParam("Ocp-Apim-Subscription-Key") String subscriptionKey);
}       