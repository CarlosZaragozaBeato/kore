package com.zensyra.ccollector.core.dto.energy;

import java.time.LocalDate;

/** Balance energético agregado de una semana (lunes a domingo). */
public record EnergyWeekDTO(
        String label,
        LocalDate from,
        LocalDate to,
        long consumedKcal,
        long burnedKcal,
        long balanceKcal,
        String state
) {
}
