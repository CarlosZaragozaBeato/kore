package com.zensyra.ccollector.core.dto.analytics;

/** Desglose de carga/volumen por clave (disciplina u origen). */
public record BreakdownDTO(
        String key,
        int workouts,
        double distanceMeters,
        long durationSeconds,
        long load
) {
}
