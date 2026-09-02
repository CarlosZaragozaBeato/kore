package com.zensyra.ccollector.core.dto.analytics;

import java.time.Instant;
import java.util.List;

/**
 * Resumen de rendimiento exportable: totales + series semanal y mensual.
 * Es un documento JSON autónomo, apto para descargar o pasar a un agente.
 */
public record DashboardDTO(
        Instant generatedAt,
        TotalsDTO totals,
        List<PeriodSummaryDTO> weekly,
        List<PeriodSummaryDTO> monthly
) {
}
