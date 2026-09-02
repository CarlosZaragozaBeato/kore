package com.zensyra.ccollector.core.dto.gym;

import com.zensyra.ccollector.core.domain.gym.ExerciseCategory;

public record ExerciseRequest(
        String name,
        String muscleGroup,
        ExerciseCategory category,
        Boolean requiresEquipment,
        String equipment,
        String description,
        String imageUrl,
        String instructions,
        Double metValue
) {
}
