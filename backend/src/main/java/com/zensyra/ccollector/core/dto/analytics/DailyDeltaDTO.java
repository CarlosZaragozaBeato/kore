package com.zensyra.ccollector.core.dto.analytics;

import java.time.LocalDate;

/**
 * Diferencia de carga entre el rango actual y el previo, alineada por posición
 * dentro del rango (día 1 vs día 1, etc.). {@code cumulativeDeltaLoad} acumula
 * la diferencia hasta ese día.
 */
public record DailyDeltaDTO(
        int dayIndex,
        LocalDate currentDate,
        LocalDate previousDate,
        long currentLoad,
        long previousLoad,
        long deltaLoad,
        long cumulativeDeltaLoad
) {
}
