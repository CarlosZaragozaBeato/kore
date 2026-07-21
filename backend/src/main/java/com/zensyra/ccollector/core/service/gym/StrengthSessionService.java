package com.zensyra.ccollector.core.service.gym;

import com.zensyra.ccollector.core.domain.gym.Routine;
import com.zensyra.ccollector.core.domain.gym.StrengthSession;
import com.zensyra.ccollector.core.dto.gym.StrengthSessionRequest;
import com.zensyra.ccollector.core.repository.gym.RoutineRepository;
import com.zensyra.ccollector.core.repository.gym.StrengthSessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class StrengthSessionService {

    private final StrengthSessionRepository sessions;
    private final RoutineRepository routines;

    public StrengthSessionService(StrengthSessionRepository sessions, RoutineRepository routines) {
        this.sessions = sessions;
        this.routines = routines;
    }

    public List<StrengthSession> list(Long userId) {
        return sessions.listByUser(userId);
    }

    @Transactional
    public StrengthSession create(Long userId, StrengthSessionRequest req) {
        validate(req);
        StrengthSession s = new StrengthSession();
        s.userId = userId;
        s.createdAt = Instant.now();
        apply(userId, s, req);
        sessions.persist(s);
        return s;
    }

    @Transactional
    public StrengthSession update(Long userId, Long id, StrengthSessionRequest req) {
        validate(req);
        StrengthSession s = get(userId, id);
        apply(userId, s, req);
        return s;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        sessions.delete(get(userId, id));
    }

    private StrengthSession get(Long userId, Long id) {
        return sessions.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Sesión de fuerza no encontrada"));
    }

    private void apply(Long userId, StrengthSession s, StrengthSessionRequest req) {
        s.date = req.date();
        s.notes = req.notes() == null || req.notes().isBlank() ? null : req.notes().trim();
        if (req.routineId() != null) {
            Routine routine = routines.findByIdAndUser(req.routineId(), userId)
                    .orElseThrow(() -> new BadRequestException("La rutina indicada no existe"));
            s.routineId = routine.id;
            s.routineName = routine.name;
        } else {
            s.routineId = null;
            s.routineName = null;
        }
    }

    private void validate(StrengthSessionRequest req) {
        if (req == null || req.date() == null) {
            throw new BadRequestException("La sesión de fuerza necesita una fecha");
        }
    }
}
