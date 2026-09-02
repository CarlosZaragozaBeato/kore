package com.zensyra.ccollector.core.client.suunto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Un entreno tal y como lo devuelve la Cloud API de Suunto (/v2/workouts y
 * webhooks de workout). Esquema validado contra la documentación oficial y una
 * carga real (50 entrenos): la FC va ANIDADA en {@code hrdata}, no como campo
 * plano — el intento previo con {@code hravg} dejaba la FC siempre a null.
 *
 * Unidades: {@code startTime} epoch-millis; {@code totalDistance} metros;
 * {@code totalTime} segundos; {@code energyConsumption} kcal; FC en ppm;
 * {@code totalAscent}/{@code totalDescent} metros; {@code avgSpeed} m/s.
 * Deserialización tolerante: solo leemos lo que usamos.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SuuntoWorkout(
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
}
