package com.zensyra.ccollector.core.client.suunto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Un entreno tal y como lo devuelve Suunto. Campos según el esquema conocido de
 * la Cloud API; validar contra datos reales (ver {@link SuuntoWorkoutsResponse}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SuuntoWorkout(
        @JsonProperty("workoutKey") String workoutKey,
        @JsonProperty("activityId") Integer activityId,
        @JsonProperty("startTime") Long startTime,
        @JsonProperty("totalDistance") Double totalDistance,
        @JsonProperty("totalTime") Double totalTime,
        @JsonProperty("hravg") Double hrAvg,
        @JsonProperty("energyConsumption") Double energyConsumption
) {
}
