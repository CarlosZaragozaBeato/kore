package com.zensyra.ccollector.core.dto.energy;

import java.time.LocalDate;

/**
 * Balance energético de un día. {@code balanceKcal} = consumidas − quemadas
 * (ingesta neta). {@code state}: DEFICIT / BALANCED / SURPLUS respecto al
 * mantenimiento, o UNKNOWN si no hay objetivo de kcal.
 */
public record EnergyDayDTO(
        LocalDate date,
        long consumedKcal,
        long burnedKcal,
        long balanceKcal,
        String state
) {
}
