package com.zensyra.ccollector.core.repository.plan;

import com.zensyra.ccollector.core.domain.plan.PlannedSession;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class PlannedSessionRepository implements PanacheRepository<PlannedSession> {

    public List<PlannedSession> listByPlan(Long planId) {
        return list("planId", Sort.by("date").ascending(), planId);
    }

    public void deleteByPlan(Long planId) {
        delete("planId", planId);
    }
}
