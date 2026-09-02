package com.zensyra.ccollector.core.dto.nutrition;

import com.zensyra.ccollector.core.domain.nutrition.BaseUnit;

/**
 * Alta/edición de un ingrediente del catálogo. Sirve también como payload de
 * import de un ingrediente generado por un agente.
 */
public record IngredientRequest(
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
}
