package com.zensyra.ccollector.core.dto.plan;

import com.zensyra.ccollector.core.domain.plan.LoadStance;
import com.zensyra.ccollector.core.dto.analytics.LoadSignalsDTO;

/**
 * Recomendación de carga (Fase E): qué postura tomar en los próximos días
 * (descarga/mantener/subir) según las señales de carga, con una razón legible y
 * las señales que la sustentan.
 */
public record PlanRecommendationDTO(
        LoadStance stance,
        String label,
        String reason,
        LoadSignalsDTO signals
) {
}
