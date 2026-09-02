package com.zensyra.ccollector.core.dto.nutrition;

import com.zensyra.ccollector.core.domain.nutrition.MealType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DietPlanDTO(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        Double targetCalories,
        Double targetProtein,
        Double targetCarbs,
        Double targetFat,
        String notes,
        Instant createdAt,
        List<MealDTO> meals
) {

    public record MealDTO(LocalDate date, MealType mealType, String recipeName, String notes) {
    }
}
