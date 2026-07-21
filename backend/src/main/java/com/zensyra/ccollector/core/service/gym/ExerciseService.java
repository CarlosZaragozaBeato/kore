package com.zensyra.ccollector.core.service.gym;

import com.zensyra.ccollector.core.domain.gym.Exercise;
import com.zensyra.ccollector.core.dto.gym.ExerciseRequest;
import com.zensyra.ccollector.core.repository.gym.ExerciseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.util.List;

@ApplicationScoped
public class ExerciseService {

    private final ExerciseRepository exercises;

    public ExerciseService(ExerciseRepository exercises) {
        this.exercises = exercises;
    }

    public List<Exercise> list(Long userId) {
        return exercises.listByUser(userId);
    }

    @Transactional
    public Exercise create(Long userId, ExerciseRequest req) {
        validate(req);
        Exercise e = new Exercise();
        e.userId = userId;
        apply(e, req);
        exercises.persist(e);
        return e;
    }

    @Transactional
    public Exercise update(Long userId, Long id, ExerciseRequest req) {
        validate(req);
        Exercise e = get(userId, id);
        apply(e, req);
        return e;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        exercises.delete(get(userId, id));
    }

    private Exercise get(Long userId, Long id) {
        return exercises.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Ejercicio no encontrado"));
    }

    private void apply(Exercise e, ExerciseRequest req) {
        e.name = req.name().trim();
        e.muscleGroup = blankToNull(req.muscleGroup());
        e.equipment = blankToNull(req.equipment());
        e.description = blankToNull(req.description());
    }

    private void validate(ExerciseRequest req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new BadRequestException("El ejercicio necesita un nombre");
        }
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
