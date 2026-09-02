package com.zensyra.ccollector.core.dto.plan;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Plan con sus sesiones y la adherencia (planificado vs realizado).
 * {@code adherencePct} = sesiones completadas / total * 100.
 */
public record PlanDTO(
        Long id,
        String name,
        String goal,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt,
        List<PlannedSessionDTO> sessions,
        int plannedCount,
        int completedCount,
        int adherencePct
) {
}
