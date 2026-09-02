package com.zensyra.ccollector.core.dto.analytics;

/** Totales de toda la sesión. */
public record TotalsDTO(
        int workouts,
        double distanceMeters,
        long durationSeconds,
        Long avgPaceSecondsPerKm,
        double load
) {
}
