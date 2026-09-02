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

    public Optional<Workout> findBySourceId(Long userId, com.zensyra.ccollector.core.domain.workout.WorkoutSource source, String sourceId) {
        return find("userId = ?1 and source = ?2 and sourceId = ?3", userId, source, sourceId).firstResultOptional();
    }

    /**
     * Borra los entrenos de un origen anteriores a una fecha (retención mínima:
     * los datos de Suunto son recuperables, no los guardamos indefinidamente).
     * Devuelve cuántos se borraron. No toca los entrenos manuales.
     */
    public long deleteBySourceBefore(Long userId, com.zensyra.ccollector.core.domain.workout.WorkoutSource source, LocalDate before) {
        return delete("userId = ?1 and source = ?2 and date < ?3", userId, source, before);
    }

    /** Entrenos del usuario en una fecha (para emparejar con lo planificado). */
    public List<Workout> listByUserAndDate(Long userId, LocalDate date) {
        return list("userId = ?1 and date = ?2", userId, date);
    }

    /** Entrenos del usuario en un rango de fechas [from, to] (para resumen de bloque). */
    public List<Workout> listByUserBetween(Long userId, LocalDate from, LocalDate to) {
        return list("userId = ?1 and date >= ?2 and date <= ?3", userId, from, to);
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
