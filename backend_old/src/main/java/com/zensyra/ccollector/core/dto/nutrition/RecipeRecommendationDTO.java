package com.zensyra.ccollector.core.dto.nutrition;

import java.util.List;

/**
 * Recetas sugeridas en función del contexto del atleta (entreno reciente,
 * actividad diaria y objetivo de peso). Documento crudo de solo lectura: la
 * app y los agentes lo leen para proponer qué comer.
 */
public record RecipeRecommendationDTO(
        Context context,
        List<Ranked> recommendations
) {

    /**
     * Contexto que dispara la recomendación.
     *
     * @param sessionsLast7    entrenos en los últimos 7 días
     * @param trainingKcalLast7 kcal de entreno acumuladas (las conocidas)
     * @param activityKcalLast7 quema diaria total registrada (si la hay)
     * @param intensity        HIGH / MODERATE / REST según carga
     * @param focus            CARB (repostaje) / BALANCED / PROTEIN (recuperación)
     * @param targetCalories   objetivo del día si hay mantenimiento fijado
     * @param rationale        explicación legible de por qué se sugiere ese enfoque
     */
    public record Context(
            int sessionsLast7,
            double trainingKcalLast7,
            Double activityKcalLast7,
            String intensity,
            String focus,
            Integer targetCalories,
            String rationale
    ) {
    }

    /** Una receta puntuada para el contexto. `score` mayor = mejor encaje. */
    public record Ranked(
            Long recipeId,
            String name,
            Double calories,
            Double protein,
            Double carbs,
            Double fat,
            double score,
            String reason
    ) {
    }
}
