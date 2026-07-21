package com.zensyra.ccollector.core.dto.workout;

import com.zensyra.ccollector.core.domain.workout.WorkoutType;

import java.time.LocalDate;

/** Payload de creación/actualización de un entrenamiento manual. */
public record WorkoutRequest(
        LocalDate date,
        WorkoutType type,
        Double distanceMeters,
        Long durationSeconds,
        Integer avgHeartRate,
        Integer perceivedEffort,
        String notes
) {
}
