package com.zensyra.ccollector.core.dto.plan;

import com.zensyra.ccollector.core.domain.plan.PlannedSession;
import com.zensyra.ccollector.core.domain.plan.SessionStatus;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;

import java.time.LocalDate;
import java.util.List;

/** Sesión planificada con su estado de cumplimiento (derivado), estado y pasos. */
public record PlannedSessionDTO(
        Long id,
        Long planId,
        LocalDate date,
        WorkoutType type,
        Double targetDistanceMeters,
        Long targetDurationSeconds,
        String description,
        boolean done,
        SessionStatus status,
        String variantGroup,
        String variantLabel,
        List<PlannedStepDTO> steps
) {

    public static PlannedSessionDTO from(PlannedSession s, boolean done, List<PlannedStepDTO> steps) {
        return new PlannedSessionDTO(
                s.id, s.planId, s.date, s.type, s.targetDistanceMeters, s.targetDurationSeconds,
                s.description, done, s.status, s.variantGroup, s.variantLabel, steps);
    }
}
