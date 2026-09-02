package com.zensyra.ccollector.core.repository.weight;

import com.zensyra.ccollector.core.domain.weight.WeightEntry;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class WeightEntryRepository implements PanacheRepository<WeightEntry> {

    /** Más recientes primero. */
    public List<WeightEntry> listByUser(Long userId) {
        return list("userId", Sort.by("date").descending(), userId);
    }

    public Optional<WeightEntry> findByIdAndUser(Long id, Long userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }

    public Optional<WeightEntry> findByDateAndUser(LocalDate date, Long userId) {
        return find("date = ?1 and userId = ?2", date, userId).firstResultOptional();
    }
}
