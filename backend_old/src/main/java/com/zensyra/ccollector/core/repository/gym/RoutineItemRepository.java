package com.zensyra.ccollector.core.repository.gym;

import com.zensyra.ccollector.core.domain.gym.RoutineItem;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class RoutineItemRepository implements PanacheRepository<RoutineItem> {

    public List<RoutineItem> listByRoutine(Long routineId) {
        return list("routineId", Sort.by("position").ascending(), routineId);
    }

    public void deleteByRoutine(Long routineId) {
        delete("routineId", routineId);
    }
}
