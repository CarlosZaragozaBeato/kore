package com.zensyra.ccollector.core.dto.analytics;

import java.time.Instant;
import java.util.List;

/**
 * Comparativa de carga entre dos rangos (por defecto semana actual vs previa),
 * con desgloses, diferencias diarias y acumuladas, y señales de sobrecarga.
 * Documento crudo, exportable como JSON.
 */
public record LoadComparisonDTO(
        Instant generatedAt,
        RangeStatsDTO current,
        RangeStatsDTO previous,
        List<DailyDeltaDTO> dailyDeltas,
        LoadSignalsDTO signals
) {
}
