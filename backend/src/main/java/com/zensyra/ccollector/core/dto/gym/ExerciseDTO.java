package com.zensyra.ccollector.core.dto.gym;

import com.zensyra.ccollector.core.domain.gym.Exercise;

public record ExerciseDTO(Long id, String name, String muscleGroup, String equipment, String description) {

    public static ExerciseDTO from(Exercise e) {
        return new ExerciseDTO(e.id, e.name, e.muscleGroup, e.equipment, e.description);
    }
}
