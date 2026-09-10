package com.zensyra.suunto.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SuuntoWorkoutDto(
        @JsonProperty("workoutKey") String workoutKey,
        @JsonProperty("activityId") Integer activityId,
        @JsonProperty("startTime") Long startTime,
        @JsonProperty("totalDistance") Double totalDistance,
        @JsonProperty("totalTime") Double totalTime,
        @JsonProperty("totalAscent") Double totalAscent,
        @JsonProperty("totalDescent") Double totalDescent,
        @JsonProperty("energyConsumption") Double energyConsumption,
        @JsonProperty("hrdata") HrData hrData,
        @JsonProperty("stepCount") Integer stepCount
) {

    /** Bloque de frecuencia cardiaca anidado dentro del workout. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HrData(
            @JsonProperty("workoutAvgHR") Double workoutAvgHR,
            @JsonProperty("workoutMaxHR") Double workoutMaxHR
    ) {
    }

    /** FC media en ppm, o null si el entreno no la trae. */
    public Double avgHeartRate() {
        return hrData == null ? null : hrData.workoutAvgHR();
    }

    /** FC máxima en ppm, o null si el entreno no la trae. */
    public Double maxHeartRate() {
        return hrData == null ? null : hrData.workoutMaxHR();
    }

    /** Alias para métricas alineados con las columnas en snake_case de la BD */
    public Double distanceMeters() {
        return totalDistance;
    }

    public Double durationSeconds() {
        return totalTime;
    }

    public Double ascentMeters() {
        return totalAscent;
    }

    public Double descentMeters() {
        return totalDescent;
    }

    /** Convierte el timestamp Unix (ms) a OffsetDateTime en UTC */
    public OffsetDateTime startTimeAsOffsetDateTime() {
        return startTime == null ? null : Instant.ofEpochMilli(startTime).atOffset(ZoneOffset.UTC);
    }
}