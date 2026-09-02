package com.zensyra.ccollector.core.dto.plan;

import java.util.List;

/**
 * Resultado de generar variantes de carga para una sesión (Fase E): la sesión
 * base más las hermanas de descarga y subida creadas como propuestas del mismo
 * grupo, junto con la recomendación de qué postura seguir según la carga actual.
 */
public record VariantSuggestionDTO(
        Long baseSessionId,
        String variantGroup,
        PlanRecommendationDTO recommendation,
        List<PlannedSessionDTO> variants
) {
}
