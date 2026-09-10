package com.zensyra.suunto.scheduler;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zensyra.domain.workout.entity.WorkoutEntity;
import com.zensyra.suunto.client.SuuntoApiClient;
import com.zensyra.suunto.dto.SuuntoWorkoutDto;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SuuntoSyncScheduler {

    @Inject
    @RestClient
    SuuntoApiClient suuntoApiClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "suunto.api.authorization")
    String authorization;

    @ConfigProperty(name = "suunto.api.subscription-key")
    String subscriptionKey;

    @Scheduled(every = "1h", delayed = "10s")
    @Transactional
    public void syncWorkouts() {
        String rawResponse = suuntoApiClient.listWorkouts(authorization, subscriptionKey);

        for (RawWorkout workout : parseWorkouts(rawResponse)) {
            WorkoutEntity.updateOrInsert(
                    workout.dto(),
                    workout.rawPayload()
            );
        }
    }

    private List<RawWorkout> parseWorkouts(String rawResponse) {
        try (JsonParser parser = objectMapper.getFactory().createParser(rawResponse)) {
            List<RawWorkout> workouts = new ArrayList<>();

            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw new IllegalStateException("Invalid Suunto workouts response");
            }

            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME
                        && "payload".equals(parser.currentName())) {

                    if (parser.nextToken() != JsonToken.START_ARRAY) {
                        throw new IllegalStateException("Suunto payload is not an array");
                    }

                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        if (parser.currentToken() != JsonToken.START_OBJECT) {
                            throw new IllegalStateException("Suunto workout is not an object");
                        }

                        long startOffset = parser.getCurrentLocation().getCharOffset();

                        SuuntoWorkoutDto dto = objectMapper.readValue(
                                parser,
                                SuuntoWorkoutDto.class
                        );

                        long endOffset = parser.getCurrentLocation().getCharOffset();

                        String rawWorkout = rawResponse.substring(
                                (int) startOffset,
                                (int) endOffset
                        );

                        workouts.add(new RawWorkout(dto, rawWorkout));
                    }

                    break;
                }
            }

            return workouts;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to parse Suunto workouts response",
                    e
            );
        }
    }

    private record RawWorkout(
            SuuntoWorkoutDto dto,
            String rawPayload
    ) {
    }
}