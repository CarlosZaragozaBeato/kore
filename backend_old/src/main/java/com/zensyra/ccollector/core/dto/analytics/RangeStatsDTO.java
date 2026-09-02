package com.zensyra.ccollector.core.dto.analytics;

import java.time.LocalDate;
import java.util.List;

/** Estadísticas de un rango de fechas: totales + series diaria + desgloses. */
public record RangeStatsDTO(
        String label,
        LocalDate from,
        LocalDate to,
        int workouts,
        double distanceMeters,
        long durationSeconds,
        long load,
        List<DaySummaryDTO> days,
        List<BreakdownDTO> byDiscipline,
        List<BreakdownDTO> bySource
) {
}
