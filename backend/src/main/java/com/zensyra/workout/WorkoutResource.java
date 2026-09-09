package com.zensyra.workout;

import com.zensyra.kore_domain.external.model.Workout;
import com.zensyra.suunto.client.SuuntoApiClient;
import com.zensyra.suunto.client.SuuntoAuthTokenProvider;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@Path("/workout")
public class WorkoutResource {

    @Inject
    SuuntoAuthTokenProvider tokenProvider;

    @Inject
    @RestClient
    SuuntoApiClient apiClient;

    @ConfigProperty(name = "suunto.subscription-key")
    String subscriptionKey;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Workout> getWorkouts() {
        String token = tokenProvider.getValidAccessToken();
        String authHeader = "Bearer " + token;

        var response = apiClient.listWorkouts(authHeader, subscriptionKey);
        if (response == null || response.workoutsOrEmpty().isEmpty()) {
            return List.of();
        }

        return response.workoutsOrEmpty().stream()
                .map(Workout::fromSuuntoDto)
                .toList();
    }
}