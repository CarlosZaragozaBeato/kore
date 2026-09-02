package com.zensyra.ccollector.core.dto.nutrition;

import java.time.Instant;
import java.util.List;

public record RecipeDTO(
        Long id,
        String name,
        String description,
        Integer servings,
        Double calories,
        Double protein,
        Double carbs,
        Double fat,
        String steps,
        Instant createdAt,
        List<IngredientDTO> ingredients
) {

    public record IngredientDTO(String name, Double quantity, String unit, Long ingredientId) {
    }
}
