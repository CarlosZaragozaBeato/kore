package com.zensyra.workout;

import com.zensyra.suunto.client.SuuntoAuthTokenProvider;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/workout")
public class WorkoutResource {

    @Inject
    SuuntoAuthTokenProvider tokenProvider;

    @GET
    @Path("/token-test")
    @Produces(MediaType.TEXT_PLAIN)
    public String testToken() {
        return tokenProvider.getValidAccessToken();
    }
}