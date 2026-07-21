package com.zensyra.ccollector.core.dto.plan;

import com.zensyra.ccollector.core.domain.workout.WorkoutType;

import java.time.LocalDate;
import java.util.List;

/**
 * Alta/edición de un plan (con sus sesiones). También es el formato que un
 * agente puede generar y enviar a POST /plans para "importar" un plan.
 */
public record PlanRequest(
        String name,
        String goal,
        LocalDate startDate,
        LocalDate endDate,
        List<SessionRequest> sessions
) {

    public record SessionRequest(
            LocalDate date,
            WorkoutType type,
            Double targetDistanceMeters,
            Long targetDurationSeconds,
            String description
    ) {
    }
}
