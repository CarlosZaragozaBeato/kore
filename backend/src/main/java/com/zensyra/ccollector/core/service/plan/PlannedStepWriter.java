package com.zensyra.ccollector.core.service.plan;

import com.zensyra.ccollector.core.domain.plan.PlannedStep;
import com.zensyra.ccollector.core.domain.plan.StepKind;
import com.zensyra.ccollector.core.dto.plan.PlanRequest;
import com.zensyra.ccollector.core.repository.plan.PlannedStepRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;

import java.util.List;

/** Persiste los pasos estructurados de una sesión planificada (plan o suelta). */
@ApplicationScoped
public class PlannedStepWriter {

    private final PlannedStepRepository steps;

    public PlannedStepWriter(PlannedStepRepository steps) {
        this.steps = steps;
    }

    public void persist(Long sessionId, List<PlanRequest.StepRequest> reqs) {
        if (reqs == null) {
            return;
        }
        int order = 0;
        for (PlanRequest.StepRequest sr : reqs) {
            PlannedStep step = new PlannedStep();
            step.plannedSessionId = sessionId;
            step.orderIndex = order++;
            step.kind = sr.kind() == null ? StepKind.STEADY : sr.kind();
            step.repeat = sr.repeat() == null || sr.repeat() < 1 ? 1 : sr.repeat();
            step.targetDistanceMeters = nonNegative(sr.targetDistanceMeters(), "la distancia objetivo");
            step.targetDurationSeconds = nonNegative(sr.targetDurationSeconds(), "la duración objetivo");
            step.targetPaceMinSecPerKm = sr.targetPaceMinSecPerKm();
            step.targetPaceMaxSecPerKm = sr.targetPaceMaxSecPerKm();
            step.targetHrMin = sr.targetHrMin();
            step.targetHrMax = sr.targetHrMax();
            step.recoverySeconds = nonNegative(sr.recoverySeconds(), "el descanso");
            step.note = sr.note() == null || sr.note().isBlank() ? null : sr.note().trim();
            steps.persist(step);
        }
    }

    private Double nonNegative(Double value, String label) {
        if (value != null && value < 0) {
            throw new BadRequestException("En un paso, " + label + " no puede ser negativa");
        }
        return value;
    }

    private Long nonNegative(Long value, String label) {
        if (value != null && value < 0) {
            throw new BadRequestException("En un paso, " + label + " no puede ser negativo");
        }
        return value;
    }
}
