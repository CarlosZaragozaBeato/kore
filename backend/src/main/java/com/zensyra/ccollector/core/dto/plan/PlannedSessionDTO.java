package com.zensyra.ccollector.core.dto.plan;

import com.zensyra.ccollector.core.domain.plan.PlannedSession;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;

import java.time.LocalDate;

/** Sesión planificada con su estado de cumplimiento (derivado). */
public record PlannedSessionDTO(
        Long id,
        LocalDate date,
        WorkoutType type,
        Double targetDistanceMeters,
        Long targetDurationSeconds,
        String description,
        boolean done
) {

    public static PlannedSessionDTO from(PlannedSession s, boolean done) {
        return new PlannedSessionDTO(
                s.id, s.date, s.type, s.targetDistanceMeters, s.targetDurationSeconds, s.description, done);
    }
}
