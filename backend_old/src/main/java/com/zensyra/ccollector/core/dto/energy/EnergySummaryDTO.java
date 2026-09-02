package com.zensyra.ccollector.core.dto.energy;

import java.time.Instant;
import java.util.List;

/**
 * Resumen de balance energético: series diaria (últimos días) y semanal, más el
 * kcal/día de mantenimiento contra el que se evalúa el estado (null si no hay
 * objetivo fijado).
 */
public record EnergySummaryDTO(
        Instant generatedAt,
        Double maintenanceKcal,
        List<EnergyDayDTO> days,
        List<EnergyWeekDTO> weeks
) {
}
