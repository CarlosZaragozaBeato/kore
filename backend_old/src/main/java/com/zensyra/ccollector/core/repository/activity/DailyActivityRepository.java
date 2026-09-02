package com.zensyra.ccollector.core.repository.activity;

import com.zensyra.ccollector.core.domain.activity.DailyActivity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class DailyActivityRepository implements PanacheRepository<DailyActivity> {

    /** Más recientes primero. */
    public List<DailyActivity> listByUser(Long userId) {
        return list("userId", Sort.by("date").descending(), userId);
    }

    public Optional<DailyActivity> findByIdAndUser(Long id, Long userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }

    public Optional<DailyActivity> findByDateAndUser(LocalDate date, Long userId) {
        return find("date = ?1 and userId = ?2", date, userId).firstResultOptional();
    }
}
