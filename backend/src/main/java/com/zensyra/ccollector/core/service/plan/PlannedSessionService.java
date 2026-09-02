package com.zensyra.ccollector.core.service.plan;

import com.zensyra.ccollector.core.domain.plan.PlannedSession;
import com.zensyra.ccollector.core.domain.plan.SessionStatus;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;
import com.zensyra.ccollector.core.dto.plan.PlannedSessionDTO;
import com.zensyra.ccollector.core.dto.plan.PlannedSessionRequest;
import com.zensyra.ccollector.core.dto.plan.PlannedStepDTO;
import com.zensyra.ccollector.core.repository.plan.PlannedSessionRepository;
import com.zensyra.ccollector.core.repository.plan.PlannedStepRepository;
import com.zensyra.ccollector.core.repository.workout.WorkoutRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Planificación suelta en el calendario (Fase C): sesiones no ligadas a un plan,
 * con estado propuesta/aceptada/descartada y variantes por día.
 */
@ApplicationScoped
public class PlannedSessionService {

    private final PlannedSessionRepository sessions;
    private final PlannedStepRepository steps;
    private final PlannedStepWriter stepWriter;
    private final WorkoutRepository workouts;

    public PlannedSessionService(PlannedSessionRepository sessions, PlannedStepRepository steps,
                                 PlannedStepWriter stepWriter, WorkoutRepository workouts) {
        this.sessions = sessions;
        this.steps = steps;
        this.stepWriter = stepWriter;
        this.workouts = workouts;
    }

    public List<PlannedSessionDTO> list(Long userId) {
        Set<LocalDate> doneDates = workouts.datesByUser(userId);
        return sessions.listStandaloneByUser(userId).stream()
                .map(s -> toDTO(s, doneDates))
                .toList();
    }

    @Transactional
    public PlannedSessionDTO create(Long userId, PlannedSessionRequest req) {
        validate(req);
        PlannedSession s = new PlannedSession();
        s.userId = userId;
        s.planId = null;
        apply(s, req);
        sessions.persist(s);
        stepWriter.persist(s.id, req.steps());
        return toDTO(s, workouts.datesByUser(userId));
    }

    @Transactional
    public PlannedSessionDTO update(Long userId, Long id, PlannedSessionRequest req) {
        validate(req);
        PlannedSession s = standalone(userId, id);
        apply(s, req);
        steps.deleteBySession(s.id);
        stepWriter.persist(s.id, req.steps());
        return toDTO(s, workouts.datesByUser(userId));
    }

    /** Acepta una variante: pasa a ACCEPTED y descarta el resto de su grupo. */
    @Transactional
    public PlannedSessionDTO accept(Long userId, Long id) {
        PlannedSession s = standalone(userId, id);
        s.status = SessionStatus.ACCEPTED;
        if (s.variantGroup != null) {
            for (PlannedSession sibling : sessions.listVariantSiblings(userId, s.variantGroup, s.id)) {
                sibling.status = SessionStatus.REJECTED;
            }
        }
        return toDTO(s, workouts.datesByUser(userId));
    }

    @Transactional
    public PlannedSessionDTO reject(Long userId, Long id) {
        PlannedSession s = standalone(userId, id);
        s.status = SessionStatus.REJECTED;
        return toDTO(s, workouts.datesByUser(userId));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        PlannedSession s = standalone(userId, id);
        steps.deleteBySession(s.id);
        sessions.delete(s);
    }

    /** Localiza una sesión suelta (sin plan) del usuario; las de plan se editan por su plan. */
    private PlannedSession standalone(Long userId, Long id) {
        PlannedSession s = sessions.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Sesión planificada no encontrada"));
        if (s.planId != null) {
            throw new BadRequestException("Esta sesión pertenece a un plan; edítala desde su plan");
        }
        return s;
    }

    private void apply(PlannedSession s, PlannedSessionRequest req) {
        s.date = req.date();
        s.type = req.type() == null ? WorkoutType.RUNNING : req.type();
        s.targetDistanceMeters = req.targetDistanceMeters();
        s.targetDurationSeconds = req.targetDurationSeconds();
        s.description = req.description() == null || req.description().isBlank() ? null : req.description().trim();
        s.status = req.status() != null ? req.status() : SessionStatus.PROPOSED;
        s.variantGroup = blankToNull(req.variantGroup());
        s.variantLabel = blankToNull(req.variantLabel());
    }

    private String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private void validate(PlannedSessionRequest req) {
        if (req == null || req.date() == null) {
            throw new BadRequestException("La sesión planificada necesita una fecha");
        }
    }

    private PlannedSessionDTO toDTO(PlannedSession s, Set<LocalDate> doneDates) {
        List<PlannedStepDTO> stepDTOs = steps.listBySession(s.id).stream()
                .map(PlannedStepDTO::from).toList();
        return PlannedSessionDTO.from(s, doneDates.contains(s.date), stepDTOs);
    }
}
