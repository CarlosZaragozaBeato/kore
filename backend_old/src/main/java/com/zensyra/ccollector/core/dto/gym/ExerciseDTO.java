package com.zensyra.ccollector.core.dto.gym;

import com.zensyra.ccollector.core.domain.gym.Exercise;
import com.zensyra.ccollector.core.domain.gym.ExerciseCategory;

public record ExerciseDTO(
        Long id,
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

    public static ExerciseDTO from(Exercise e) {
        return new ExerciseDTO(e.id, e.name, e.muscleGroup, e.category,
                e.requiresEquipment, e.equipment, e.description,
                e.imageUrl, e.instructions, e.metValue);
    }
}
