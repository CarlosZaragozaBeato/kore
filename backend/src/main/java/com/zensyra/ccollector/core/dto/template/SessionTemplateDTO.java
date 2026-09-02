package com.zensyra.ccollector.core.dto.template;

import com.zensyra.ccollector.core.domain.template.Level;
import com.zensyra.ccollector.core.domain.template.RaceGoal;
import com.zensyra.ccollector.core.domain.template.SessionTemplate;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;

public record SessionTemplateDTO(
        Long id,
        String name,
        WorkoutType discipline,
        RaceGoal goal,
        Level level,
        Double targetDistanceMeters,
        Long targetDurationSeconds,
        String structure,
        String notes
) {

    public static SessionTemplateDTO from(SessionTemplate t) {
        return new SessionTemplateDTO(t.id, t.name, t.discipline, t.goal, t.level,
                t.targetDistanceMeters, t.targetDurationSeconds, t.structure, t.notes);
    }
}
