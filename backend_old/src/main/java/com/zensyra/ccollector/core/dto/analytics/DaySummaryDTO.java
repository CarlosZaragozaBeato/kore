package com.zensyra.ccollector.core.dto.analytics;

import java.time.LocalDate;

/** Volumen y carga de un día concreto. */
public record DaySummaryDTO(
        LocalDate date,
        double distanceMeters,
        long durationSeconds,
        long load
) {
}
