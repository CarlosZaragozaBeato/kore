package com.zensyra.ccollector.core.dto.nutrition;

import com.zensyra.ccollector.core.domain.nutrition.MealType;

import java.time.LocalDate;
import java.util.List;

/** Alta/edición de un plan de dieta con sus comidas (también sirve de import). */
public record DietPlanRequest(
        String name,
        LocalDate startDate,
        LocalDate endDate,
        Double targetCalories,
        Double targetProtein,
        Double targetCarbs,
        Double targetFat,
        String notes,
        List<MealRequest> meals
) {

    public record MealRequest(LocalDate date, MealType mealType, String recipeName, String notes) {
    }
}
