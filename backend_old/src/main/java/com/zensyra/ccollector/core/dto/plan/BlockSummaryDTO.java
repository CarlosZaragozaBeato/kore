package com.zensyra.ccollector.core.dto.plan;

import java.time.LocalDate;

/**
 * Resumen <b>derivado</b> de un bloque: agrega sobre su rango de fechas las
 * sesiones planificadas y los entrenos realizados para contrastar el plan con
 * lo hecho. No persiste nada.
 */
public record BlockSummaryDTO(
        Long blockId,
        LocalDate startDate,
        LocalDate endDate,
        int weeks,
        int plannedSessions,
        int completedWorkouts,
        double plannedDistanceMeters,
        double completedDistanceMeters,
        Integer adherencePct
) {
}
