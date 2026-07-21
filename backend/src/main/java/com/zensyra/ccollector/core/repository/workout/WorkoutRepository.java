package com.zensyra.ccollector.core.repository.workout;

import com.zensyra.ccollector.core.domain.workout.Workout;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class WorkoutRepository implements PanacheRepository<Workout> {

    public List<Workout> listByUser(Long userId) {
        return list("userId", Sort.by("date").descending().and("id", Sort.Direction.Descending), userId);
    }

    public Optional<Workout> findByIdAndUser(Long id, Long userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }

    public boolean existsBySourceId(Long userId, com.zensyra.ccollector.core.domain.workout.WorkoutSource source, String sourceId) {
        return count("userId = ?1 and source = ?2 and sourceId = ?3", userId, source, sourceId) > 0;
    }

    /** Días en los que el usuario tiene al menos un entreno (para adherencia). */
    public Set<LocalDate> datesByUser(Long userId) {
        return getEntityManager()
                .createQuery("select distinct w.date from Workout w where w.userId = ?1", LocalDate.class)
                .setParameter(1, userId)
                .getResultStream()
                .collect(Collectors.toSet());
    }
}
