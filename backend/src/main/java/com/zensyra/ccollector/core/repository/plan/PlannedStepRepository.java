package com.zensyra.ccollector.core.repository.plan;

import com.zensyra.ccollector.core.domain.plan.PlannedStep;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class PlannedStepRepository implements PanacheRepository<PlannedStep> {

    public List<PlannedStep> listBySession(Long plannedSessionId) {
        return list("plannedSessionId", Sort.by("orderIndex").ascending(), plannedSessionId);
    }

    /** Borra los pasos de todas las sesiones de un plan (para reemplazo en update). */
    public void deleteByPlan(Long planId) {
        delete("plannedSessionId in (select s.id from PlannedSession s where s.planId = ?1)", planId);
    }

    public void deleteBySession(Long plannedSessionId) {
        delete("plannedSessionId", plannedSessionId);
    }
}
