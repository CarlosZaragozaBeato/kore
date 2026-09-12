package com.zensyra.suunto.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "suunto-api")
public interface SuuntoApiClient {

    @GET
    @Path("/v3/workouts")
    @Produces(MediaType.APPLICATION_JSON)
    String listWorkouts(
            @HeaderParam("Authorization") String authorization,
            @HeaderParam("Ocp-Apim-Subscription-Key") String subscriptionKey,
            @QueryParam("since") long since,
            @QueryParam("until") long until,
            @QueryParam("limit") int limit,
            @QueryParam("offset") int offset,
            @QueryParam("filter-by-modification-time") boolean filterByModificationTime);
}
