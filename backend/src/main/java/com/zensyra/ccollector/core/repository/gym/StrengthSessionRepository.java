package com.zensyra.ccollector.core.repository.gym;

import com.zensyra.ccollector.core.domain.gym.StrengthSession;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class StrengthSessionRepository implements PanacheRepository<StrengthSession> {

    public List<StrengthSession> listByUser(Long userId) {
        return list("userId", Sort.by("date").descending().and("id", Sort.Direction.Descending), userId);
    }

    public Optional<StrengthSession> findByIdAndUser(Long id, Long userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }
}
