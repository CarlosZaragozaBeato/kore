package com.zensyra.ccollector.core.dto.plan;

import com.zensyra.ccollector.core.domain.plan.PlannedStep;
import com.zensyra.ccollector.core.domain.plan.StepKind;

/** Un paso estructurado de una sesión planificada. */
public record PlannedStepDTO(
        Long id,
        int orderIndex,
        StepKind kind,
        int repeat,
        Double targetDistanceMeters,
        Long targetDurationSeconds,
        Integer targetPaceMinSecPerKm,
        Integer targetPaceMaxSecPerKm,
        Integer targetHrMin,
        Integer targetHrMax,
        Long recoverySeconds,
        String note
) {

    public static PlannedStepDTO from(PlannedStep s) {
        return new PlannedStepDTO(
                s.id, s.orderIndex, s.kind, s.repeat,
                s.targetDistanceMeters, s.targetDurationSeconds,
                s.targetPaceMinSecPerKm, s.targetPaceMaxSecPerKm,
                s.targetHrMin, s.targetHrMax, s.recoverySeconds, s.note);
    }
}
