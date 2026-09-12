package com.zensyra.suunto.scheduler;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zensyra.domain.sync.entity.SyncStateEntity;
import com.zensyra.domain.workout.entity.WorkoutEntity;
import com.zensyra.suunto.client.SuuntoApiClient;
import com.zensyra.suunto.client.SuuntoAuthTokenProvider;
import com.zensyra.suunto.dto.SuuntoWorkoutDto;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SuuntoSyncScheduler {

    private static final String SOURCE = "suunto";
    private static final int PAGE_SIZE = 50;

    @Inject
    @RestClient
    SuuntoApiClient suuntoApiClient;

    @Inject
    SuuntoAuthTokenProvider authTokenProvider;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "suunto.subscription-key")
    String subscriptionKey;

    @Scheduled(every = "1h", delayed = "10s")
    @Transactional
    public void syncWorkouts() {
        SyncStateEntity syncState = SyncStateEntity.getOrCreate(SOURCE);

        long since = syncState.lastSyncedAt;
        long until = Instant.now().toEpochMilli();

        String accessToken = authTokenProvider.getValidAccessToken();

        int offset = 0;

        while (true) {
            String rawResponse = suuntoApiClient.listWorkouts(
                    "Bearer " + accessToken,
                    subscriptionKey,
                    since,
                    until,
                    PAGE_SIZE,
                    offset,
                    true
            );

            List<RawWorkout> workouts = parseWorkouts(rawResponse);

            for (RawWorkout workout : workouts) {
                WorkoutEntity.updateOrInsert(
                        workout.dto(),
                        workout.rawPayload()
                );
            }

            if (workouts.size() < PAGE_SIZE) {
                break;
            }

            offset += PAGE_SIZE;
        }

        syncState.lastSyncedAt = until;
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

                    JsonToken payloadToken = parser.nextToken();

                    if (payloadToken != JsonToken.START_ARRAY) {
                        throw new IllegalStateException("Suunto payload is not an array");
                    }

                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        if (parser.currentToken() != JsonToken.START_OBJECT) {
                            throw new IllegalStateException(
                                    "Suunto workout is not an object: "
                                            + parser.currentToken()
                            );
                        }

                        long startOffset = parser.getTokenLocation().getCharOffset();

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