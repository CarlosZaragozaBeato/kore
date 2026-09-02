package com.zensyra.ccollector.core.dto.analytics;

import java.time.LocalDate;

/**
 * Resumen agregado de un periodo (semana o mes). {@code avgPaceSecondsPerKm} y
 * {@code load} son derivados.
 */
public record PeriodSummaryDTO(
        String label,
        LocalDate from,
        LocalDate to,
        int workouts,
        double distanceMeters,
        long durationSeconds,
        Long avgPaceSecondsPerKm,
        double load
) {
}
