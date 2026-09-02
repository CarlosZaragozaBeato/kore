package com.zensyra.ccollector.core.service.workout;

import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.domain.workout.WorkoutSource;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;
import com.zensyra.ccollector.core.dto.workout.WorkoutRequest;
import com.zensyra.ccollector.core.repository.workout.WorkoutRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class WorkoutService {

    private final WorkoutRepository workouts;

    public WorkoutService(WorkoutRepository workouts) {
        this.workouts = workouts;
    }

    public List<Workout> list(Long userId) {
        return workouts.listByUser(userId);
    }

    public Workout get(Long userId, Long id) {
        return workouts.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Entrenamiento no encontrado"));
    }

    @Transactional
    public Workout create(Long userId, WorkoutRequest req) {
        validate(req);
        Workout w = new Workout();
        w.userId = userId;
        w.source = WorkoutSource.MANUAL;
        w.createdAt = Instant.now();
        apply(w, req);
        workouts.persist(w);
        return w;
    }

    @Transactional
    public Workout update(Long userId, Long id, WorkoutRequest req) {
        validate(req);
        Workout w = get(userId, id);
        apply(w, req);
        return w;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Workout w = get(userId, id);
        workouts.delete(w);
    }

    private void apply(Workout w, WorkoutRequest req) {
        w.date = req.date();
        w.type = req.type() == null ? WorkoutType.RUNNING : req.type();
        w.distanceMeters = req.distanceMeters();
        w.durationSeconds = req.durationSeconds();
        w.avgHeartRate = req.avgHeartRate();
        w.maxHeartRate = req.maxHeartRate();
        w.energyKcal = req.energyKcal();
        w.stepCount = req.stepCount();
        w.perceivedEffort = req.perceivedEffort();
        w.notes = req.notes();
    }

    private void validate(WorkoutRequest req) {
        if (req == null || req.date() == null) {
            throw new BadRequestException("La fecha del entrenamiento es obligatoria");
        }
        if (req.date().isAfter(LocalDate.now())) {
            throw new BadRequestException("La fecha no puede ser futura");
        }
        if (req.distanceMeters() != null && req.distanceMeters() < 0) {
            throw new BadRequestException("La distancia no puede ser negativa");
        }
        if (req.durationSeconds() != null && req.durationSeconds() < 0) {
            throw new BadRequestException("La duración no puede ser negativa");
        }
        if (req.perceivedEffort() != null && (req.perceivedEffort() < 1 || req.perceivedEffort() > 10)) {
            throw new BadRequestException("El esfuerzo percibido debe estar entre 1 y 10");
        }
    }
}
