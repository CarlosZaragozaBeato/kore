package com.zensyra.ccollector.core.repository.plan;

import com.zensyra.ccollector.core.domain.plan.TrainingPlan;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PlanRepository implements PanacheRepository<TrainingPlan> {

    public List<TrainingPlan> listByUser(Long userId) {
        return list("userId", Sort.by("startDate").descending(), userId);
    }

    public Optional<TrainingPlan> findByIdAndUser(Long id, Long userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }
}
