package com.zensyra.ccollector.core.dto.gym;

import java.util.List;

/** Alta/edición de una rutina con sus ejercicios (también sirve de import). */
public record RoutineRequest(String name, String description, List<ItemRequest> items) {

    public record ItemRequest(
            String exerciseName,
            Integer sets,
            Integer reps,
            Integer restSeconds,
            String notes
    ) {
    }
}
