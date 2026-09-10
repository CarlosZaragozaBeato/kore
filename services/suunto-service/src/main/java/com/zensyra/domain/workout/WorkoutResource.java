package com.zensyra.domain.workout;

import com.zensyra.domain.workout.entity.WorkoutEntity;
import com.zensyra.domain.workout.model.Workout;
import com.zensyra.suunto.client.SuuntoApiClient;
import com.zensyra.suunto.client.SuuntoAuthTokenProvider;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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
    @Transactional
    public List<Workout> getWorkouts() {
        String token = tokenProvider.getValidAccessToken();
        String authHeader = "Bearer " + token;

        var response = apiClient.listWorkouts(authHeader, subscriptionKey);
        if (response == null || response.workoutsOrEmpty().isEmpty()) {
            return List.of();
        }

        var dtos = response.workoutsOrEmpty();

        // Guardado/Actualización de payloads en BD como efecto secundario
        dtos.forEach(WorkoutEntity::updateOrInsert);

        return dtos.stream()
                .map(Workout::fromSuuntoDto)
                .toList();
    }
}