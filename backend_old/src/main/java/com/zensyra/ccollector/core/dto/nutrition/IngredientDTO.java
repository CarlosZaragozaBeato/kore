package com.zensyra.ccollector.core.dto.nutrition;

import com.zensyra.ccollector.core.domain.nutrition.BaseUnit;
import com.zensyra.ccollector.core.domain.nutrition.Ingredient;

/** Ingrediente del catálogo en la API. Valores nutricionales por 100 g/ml. */
public record IngredientDTO(
        Long id,
        String name,
        BaseUnit baseUnit,
        String imageUrl,
        Double calories,
        Double protein,
        Double carbs,
        Double fat,
        Double fiber,
        Double sugars,
        Double sodium
) {

    public static IngredientDTO from(Ingredient i) {
        return new IngredientDTO(i.id, i.name, i.baseUnit, i.imageUrl, i.calories, i.protein,
                i.carbs, i.fat, i.fiber, i.sugars, i.sodium);
    }
}
