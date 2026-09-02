package com.zensyra.ccollector.core.dto.plan;

import com.zensyra.ccollector.core.domain.plan.SessionStatus;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;

import java.time.LocalDate;
import java.util.List;

/**
 * Alta/edición de una sesión planificada suelta en el calendario (Fase C).
 * Reutiliza {@link PlanRequest.StepRequest} para los pasos estructurados.
 * Si se envían varias con el mismo {@code variantGroup} para un día, son
 * variantes entre las que elegir (aceptar una descarta el resto).
 */
public record PlannedSessionRequest(
        LocalDate date,
        WorkoutType type,
        Double targetDistanceMeters,
        Long targetDurationSeconds,
        String description,
        SessionStatus status,
        String variantGroup,
        String variantLabel,
        List<PlanRequest.StepRequest> steps
) {
}
