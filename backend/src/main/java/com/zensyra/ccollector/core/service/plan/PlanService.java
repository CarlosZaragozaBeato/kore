package com.zensyra.ccollector.core.service.plan;

import com.zensyra.ccollector.core.domain.plan.PlannedSession;
import com.zensyra.ccollector.core.domain.plan.SessionStatus;
import com.zensyra.ccollector.core.domain.plan.TrainingPlan;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;
import com.zensyra.ccollector.core.dto.plan.PlanDTO;
import com.zensyra.ccollector.core.dto.plan.PlanRequest;
import com.zensyra.ccollector.core.dto.plan.PlannedSessionDTO;
import com.zensyra.ccollector.core.dto.plan.PlannedStepDTO;
import com.zensyra.ccollector.core.repository.plan.PlanRepository;
import com.zensyra.ccollector.core.repository.plan.PlannedSessionRepository;
import com.zensyra.ccollector.core.repository.plan.PlannedStepRepository;
import com.zensyra.ccollector.core.repository.workout.WorkoutRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class PlanService {

    private final PlanRepository plans;
    private final PlannedSessionRepository sessions;
    private final PlannedStepRepository steps;
    private final PlannedStepWriter stepWriter;
    private final WorkoutRepository workouts;

    public PlanService(PlanRepository plans, PlannedSessionRepository sessions,
                       PlannedStepRepository steps, PlannedStepWriter stepWriter,
                       WorkoutRepository workouts) {
        this.plans = plans;
        this.sessions = sessions;
        this.steps = steps;
        this.stepWriter = stepWriter;
        this.workouts = workouts;
    }

    public List<PlanDTO> list(Long userId) {
        Set<LocalDate> doneDates = workouts.datesByUser(userId);
        return plans.listByUser(userId).stream()
                .map(p -> toDTO(p, doneDates))
                .toList();
    }

    public PlanDTO get(Long userId, Long id) {
        TrainingPlan plan = plans.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Plan no encontrado"));
        return toDTO(plan, workouts.datesByUser(userId));
    }

    @Transactional
    public PlanDTO create(Long userId, PlanRequest req) {
        validate(req);
        TrainingPlan plan = new TrainingPlan();
        plan.userId = userId;
        plan.createdAt = Instant.now();
        applyMeta(plan, req);
        plans.persist(plan);
        replaceSessions(plan.id, userId, req);
        return toDTO(plan, workouts.datesByUser(userId));
    }

    @Transactional
    public PlanDTO update(Long userId, Long id, PlanRequest req) {
        validate(req);
        TrainingPlan plan = plans.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Plan no encontrado"));
        applyMeta(plan, req);
        replaceSessions(plan.id, userId, req);
        return toDTO(plan, workouts.datesByUser(userId));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        TrainingPlan plan = plans.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Plan no encontrado"));
        steps.deleteByPlan(plan.id);
        sessions.deleteByPlan(plan.id);
        plans.delete(plan);
    }

    private void applyMeta(TrainingPlan plan, PlanRequest req) {
        plan.name = req.name().trim();
        plan.goal = req.goal() == null || req.goal().isBlank() ? null : req.goal().trim();
        plan.startDate = req.startDate();
        plan.endDate = req.endDate();
    }

    private void replaceSessions(Long planId, Long userId, PlanRequest req) {
        steps.deleteByPlan(planId);
        sessions.deleteByPlan(planId);
        if (req.sessions() == null) {
            return;
        }
        for (PlanRequest.SessionRequest sr : req.sessions()) {
            if (sr.date() == null) {
                throw new BadRequestException("Cada sesión planificada necesita una fecha");
            }
            PlannedSession s = new PlannedSession();
            s.planId = planId;
            s.userId = userId;
            s.status = SessionStatus.ACCEPTED;
            s.date = sr.date();
            s.type = sr.type() == null ? WorkoutType.RUNNING : sr.type();
            s.targetDistanceMeters = sr.targetDistanceMeters();
            s.targetDurationSeconds = sr.targetDurationSeconds();
            s.description = sr.description();
            sessions.persist(s);
            stepWriter.persist(s.id, sr.steps());
        }
    }

    private void validate(PlanRequest req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new BadRequestException("El plan necesita un nombre");
        }
        if (req.startDate() == null) {
            throw new BadRequestException("El plan necesita una fecha de inicio");
        }
        if (req.endDate() != null && req.endDate().isBefore(req.startDate())) {
            throw new BadRequestException("La fecha de fin no puede ser anterior al inicio");
        }
    }

    private PlanDTO toDTO(TrainingPlan plan, Set<LocalDate> doneDates) {
        List<PlannedSession> planSessions = sessions.listByPlan(plan.id);
        List<PlannedSessionDTO> sessionDTOs = planSessions.stream()
                .map(s -> PlannedSessionDTO.from(s, doneDates.contains(s.date),
                        steps.listBySession(s.id).stream().map(PlannedStepDTO::from).toList()))
                .toList();
        int completed = (int) sessionDTOs.stream().filter(PlannedSessionDTO::done).count();
        int total = sessionDTOs.size();
        int pct = total == 0 ? 0 : Math.round(completed * 100f / total);
        return new PlanDTO(
                plan.id, plan.name, plan.goal, plan.startDate, plan.endDate, plan.createdAt,
                sessionDTOs, total, completed, pct);
    }
}
