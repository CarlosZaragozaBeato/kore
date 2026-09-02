package com.zensyra.ccollector.core.dto.workout;

import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.domain.workout.WorkoutSource;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Representación de un entrenamiento en la API. {@code paceSecondsPerKm} es
 * derivado (no se almacena) para comodidad del cliente.
 */
public record WorkoutDTO(
        Long id,
        LocalDate date,
        WorkoutType type,
        Double distanceMeters,
        Long durationSeconds,
        Integer avgHeartRate,
        Integer maxHeartRate,
        Double energyKcal,
        Integer stepCount,
        Integer perceivedEffort,
        String notes,
        WorkoutSource source,
        Instant createdAt,
        Long paceSecondsPerKm,
        Integer avgCadenceSpm,
        Double strideLengthMeters
) {

    public static WorkoutDTO from(Workout w) {
        return new WorkoutDTO(
                w.id, w.date, w.type, w.distanceMeters, w.durationSeconds,
                w.avgHeartRate, w.maxHeartRate, w.energyKcal, w.stepCount, w.perceivedEffort,
                w.notes, w.source, w.createdAt,
                pace(w.distanceMeters, w.durationSeconds),
                cadence(w.stepCount, w.durationSeconds),
                stride(w.distanceMeters, w.stepCount));
    }

    private static Long pace(Double meters, Long seconds) {
        if (meters == null || seconds == null || meters <= 0) {
            return null;
        }
        return Math.round(seconds / (meters / 1000.0));
    }

    /** Cadencia media en pasos/min, derivada de pasos y duración. */
    private static Integer cadence(Integer steps, Long seconds) {
        if (steps == null || steps <= 0 || seconds == null || seconds <= 0) {
            return null;
        }
        return (int) Math.round(steps / (seconds / 60.0));
    }

    /** Longitud de zancada en metros/paso, derivada de distancia y pasos. */
    private static Double stride(Double meters, Integer steps) {
        if (meters == null || meters <= 0 || steps == null || steps <= 0) {
            return null;
        }
        return Math.round(meters / steps * 100.0) / 100.0;
    }
}
