package com.zensyra.ccollector.core.dto.template;

import com.zensyra.ccollector.core.domain.template.Level;
import com.zensyra.ccollector.core.domain.template.RaceGoal;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;

/** Alta/edición de una plantilla de sesión (también sirve de import de agente). */
public record SessionTemplateRequest(
        String name,
        WorkoutType discipline,
        RaceGoal goal,
        Level level,
        Double targetDistanceMeters,
        Long targetDurationSeconds,
        String structure,
        String notes
) {
}
