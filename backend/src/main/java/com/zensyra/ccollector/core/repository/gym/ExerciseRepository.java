package com.zensyra.ccollector.core.repository.gym;

import com.zensyra.ccollector.core.domain.gym.Exercise;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ExerciseRepository implements PanacheRepository<Exercise> {

    public List<Exercise> listByUser(Long userId) {
        return list("userId", Sort.by("name").ascending(), userId);
    }

    public Optional<Exercise> findByIdAndUser(Long id, Long userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }

    public Optional<Exercise> findByNameAndUser(String name, Long userId) {
        return find("name = ?1 and userId = ?2", name, userId).firstResultOptional();
    }
}
