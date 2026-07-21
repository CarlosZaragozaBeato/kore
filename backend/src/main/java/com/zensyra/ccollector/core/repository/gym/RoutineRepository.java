package com.zensyra.ccollector.core.repository.gym;

import com.zensyra.ccollector.core.domain.gym.Routine;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RoutineRepository implements PanacheRepository<Routine> {

    public List<Routine> listByUser(Long userId) {
        return list("userId", Sort.by("name").ascending(), userId);
    }

    public Optional<Routine> findByIdAndUser(Long id, Long userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }
}
