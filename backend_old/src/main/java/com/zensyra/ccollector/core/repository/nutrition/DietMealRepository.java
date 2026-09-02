package com.zensyra.ccollector.core.repository.nutrition;

import com.zensyra.ccollector.core.domain.nutrition.DietMeal;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class DietMealRepository implements PanacheRepository<DietMeal> {

    public List<DietMeal> listByPlan(Long dietPlanId) {
        return list("dietPlanId", Sort.by("date").ascending().and("id", Sort.Direction.Ascending), dietPlanId);
    }

    public void deleteByPlan(Long dietPlanId) {
        delete("dietPlanId", dietPlanId);
    }
}
