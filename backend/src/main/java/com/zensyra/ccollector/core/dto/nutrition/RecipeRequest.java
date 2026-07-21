package com.zensyra.ccollector.core.dto.nutrition;

import java.util.List;

/** Alta/edición de receta con ingredientes (también sirve de import). */
public record RecipeRequest(
        String name,
        String description,
        Integer servings,
        Double calories,
        Double protein,
        Double carbs,
        Double fat,
        String steps,
        List<IngredientRequest> ingredients
) {

    public record IngredientRequest(String name, Double quantity, String unit) {
    }
}
