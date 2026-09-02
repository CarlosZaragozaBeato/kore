package com.zensyra.ccollector.core.dto.analytics;

/**
 * Señales de control de carga (orientativas, no diagnóstico):
 * <ul>
 *   <li><b>ACWR</b> — ratio carga aguda (7d) / crónica (media semanal de 28d).
 *       Zona OK ~0.8–1.3; alto &gt;1.5 = riesgo.</li>
 *   <li><b>Monotony</b> (Foster) — media/DE de la carga diaria de la semana.
 *       &gt;2 = poca variabilidad, riesgo.</li>
 *   <li><b>Ramp</b> — variación % de carga de la semana actual vs la previa.
 *       Subidas bruscas (&gt;50%) = riesgo.</li>
 * </ul>
 * Cada estado es OK / WARN / RISK / UNKNOWN.
 */
public record LoadSignalsDTO(
        long acuteLoad,
        long chronicLoad,
        Double acwr,
        String acwrState,
        Double monotony,
        String monotonyState,
        Double rampPct,
        String rampState
) {
}
