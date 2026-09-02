package com.zensyra.ccollector.core.repository.nutrition;

import com.zensyra.ccollector.core.domain.nutrition.DietPlan;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class DietPlanRepository implements PanacheRepository<DietPlan> {

    public List<DietPlan> listByUser(Long userId) {
        return list("userId", Sort.by("startDate").descending(), userId);
    }

    public Optional<DietPlan> findByIdAndUser(Long id, Long userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }
}
