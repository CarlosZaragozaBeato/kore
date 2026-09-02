package com.zensyra.ccollector.core.dto.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Informe de correlaciones transversales: cruza semana a semana el
 * entrenamiento (carga, km, ritmo), la nutrición (consumidas, balance), la
 * actividad diaria (pasos) y el resultado corporal (peso), y estima la relación
 * (Pearson) entre pares con sentido.
 *
 * <p>Se calcula sobre las semanas recientes que ya están almacenadas. Los
 * históricos de Suunto no se guardan a largo plazo (retención mínima), así que
 * la carga de históricos puntuales para mirar más atrás queda pendiente del
 * mismo bloqueo que la extracción por FIT/rango (Fase 18).
 */
public record CorrelationReportDTO(
        Instant generatedAt,
        int weeks,
        List<WeekPoint> series,
        List<Correlation> correlations,
        String note
) {

    /** Un punto semanal con una métrica por sección (null = sin dato esa semana). */
    public record WeekPoint(
            String label,
            LocalDate weekStart,
            Double load,
            Double km,
            Double avgPaceSecondsPerKm,
            Double consumedKcal,
            Double burnedKcal,
            Double balanceKcal,
            Long steps,
            Double weightKg
    ) {
    }

    /**
     * Correlación entre dos métricas.
     *
     * @param r              coeficiente de Pearson [-1, 1], o null si no hay datos
     *                       suficientes (mínimo 3 semanas emparejadas o sin varianza)
     * @param n              semanas con ambas métricas presentes
     * @param strength       ninguna / débil / moderada / fuerte / muy fuerte
     * @param direction      positiva / negativa / —
     * @param interpretation frase legible con el sentido de la relación
     */
    public record Correlation(
            String aLabel,
            String bLabel,
            Double r,
            int n,
            String strength,
            String direction,
            String interpretation
    ) {
    }
}
