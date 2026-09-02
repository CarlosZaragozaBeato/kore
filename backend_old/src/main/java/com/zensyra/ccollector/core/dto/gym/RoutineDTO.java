package com.zensyra.ccollector.core.dto.gym;

import java.time.Instant;
import java.util.List;

public record RoutineDTO(
        Long id,
        String name,
        String description,
        Instant createdAt,
        List<RoutineItemDTO> items
) {

    public record RoutineItemDTO(
            String exerciseName,
            Integer sets,
            Integer reps,
            Integer restSeconds,
            String notes
    ) {
    }
}
