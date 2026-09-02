package com.zensyra.ccollector.core.repository.weight;

import com.zensyra.ccollector.core.domain.weight.WeightGoal;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class WeightGoalRepository implements PanacheRepository<WeightGoal> {

    public Optional<WeightGoal> findByUser(Long userId) {
        return find("userId", userId).firstResultOptional();
    }
}
