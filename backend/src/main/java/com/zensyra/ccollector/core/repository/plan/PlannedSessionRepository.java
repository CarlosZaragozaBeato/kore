package com.zensyra.ccollector.core.repository.plan;

import com.zensyra.ccollector.core.domain.plan.PlannedSession;
import com.zensyra.ccollector.core.domain.plan.SessionStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PlannedSessionRepository implements PanacheRepository<PlannedSession> {

    public List<PlannedSession> listByPlan(Long planId) {
        return list("planId", Sort.by("date").ascending(), planId);
    }

    public void deleteByPlan(Long planId) {
        delete("planId", planId);
    }

    /** Sesiones sueltas del calendario (sin plan) de un usuario. */
    public List<PlannedSession> listStandaloneByUser(Long userId) {
        return list("planId is null and userId = ?1", Sort.by("date").ascending(), userId);
    }

    public Optional<PlannedSession> findByIdAndUser(Long id, Long userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }

    /** Sesiones planificadas (de plan o sueltas) no descartadas en un rango (resumen de bloque). */
    public List<PlannedSession> listByUserBetween(Long userId, LocalDate from, LocalDate to) {
        return list("userId = ?1 and status <> ?2 and date >= ?3 and date <= ?4",
                userId, SessionStatus.REJECTED, from, to);
    }

    /** Otras variantes del mismo grupo (para descartarlas al aceptar una). */
    public List<PlannedSession> listVariantSiblings(Long userId, String variantGroup, Long excludeId) {
        return list("userId = ?1 and variantGroup = ?2 and id <> ?3", userId, variantGroup, excludeId);
    }
}
