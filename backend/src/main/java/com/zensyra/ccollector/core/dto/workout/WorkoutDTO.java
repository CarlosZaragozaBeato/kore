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
        Integer perceivedEffort,
        String notes,
        WorkoutSource source,
        Instant createdAt,
        Long paceSecondsPerKm
) {

    public static WorkoutDTO from(Workout w) {
        return new WorkoutDTO(
                w.id, w.date, w.type, w.distanceMeters, w.durationSeconds,
                w.avgHeartRate, w.perceivedEffort, w.notes, w.source, w.createdAt,
                pace(w.distanceMeters, w.durationSeconds));
    }

    private static Long pace(Double meters, Long seconds) {
        if (meters == null || seconds == null || meters <= 0) {
            return null;
        }
        return Math.round(seconds / (meters / 1000.0));
    }
}
