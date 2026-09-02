package com.zensyra.ccollector.core.repository.plan;

import com.zensyra.ccollector.core.domain.plan.TrainingBlock;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TrainingBlockRepository implements PanacheRepository<TrainingBlock> {

    /** Todos los bloques del usuario, ordenados por nivel y fecha para armar el árbol. */
    public List<TrainingBlock> listByUser(Long userId) {
        return list("userId", Sort.by("level").and("startDate").ascending(), userId);
    }

    public Optional<TrainingBlock> findByIdAndUser(Long id, Long userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }

    public List<TrainingBlock> listChildren(Long userId, Long parentId) {
        return list("userId = ?1 and parentId = ?2", Sort.by("startDate").ascending(), userId, parentId);
    }
}
