package com.zensyra.ccollector.core.service.gym;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zensyra.ccollector.core.domain.gym.Exercise;
import com.zensyra.ccollector.core.dto.catalog.SeedResult;
import com.zensyra.ccollector.core.dto.gym.ExerciseRequest;
import com.zensyra.ccollector.core.repository.gym.ExerciseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.io.InputStream;
import java.util.List;

@ApplicationScoped
public class ExerciseService {

    private static final String SEED = "/seeds/exercises.json";

    private final ExerciseRepository exercises;
    private final ObjectMapper mapper;

    public ExerciseService(ExerciseRepository exercises, ObjectMapper mapper) {
        this.exercises = exercises;
        this.mapper = mapper;
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

    /** Carga el catálogo de ejemplo (JSON empaquetado). Idempotente por nombre. */
    @Transactional
    public SeedResult seed(Long userId) {
        List<ExerciseRequest> seeds = readSeed();
        int added = 0;
        int skipped = 0;
        for (ExerciseRequest req : seeds) {
            if (req.name() == null || req.name().isBlank()
                    || exercises.findByNameAndUser(req.name().trim(), userId).isPresent()) {
                skipped++;
                continue;
            }
            Exercise e = new Exercise();
            e.userId = userId;
            apply(e, req);
            exercises.persist(e);
            added++;
        }
        return new SeedResult(added, skipped, seeds.size());
    }

    private List<ExerciseRequest> readSeed() {
        try (InputStream in = ExerciseService.class.getResourceAsStream(SEED)) {
            if (in == null) {
                return List.of();
            }
            return mapper.readValue(in, mapper.getTypeFactory()
                    .constructCollectionType(List.class, ExerciseRequest.class));
        } catch (Exception e) {
            throw new BadRequestException("No se pudo leer el catálogo de ejemplo de ejercicios");
        }
    }

    private Exercise get(Long userId, Long id) {
        return exercises.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Ejercicio no encontrado"));
    }

    private void apply(Exercise e, ExerciseRequest req) {
        e.name = req.name().trim();
        e.muscleGroup = blankToNull(req.muscleGroup());
        e.category = req.category();
        e.requiresEquipment = req.requiresEquipment();
        e.equipment = blankToNull(req.equipment());
        e.description = blankToNull(req.description());
        e.imageUrl = blankToNull(req.imageUrl());
        e.instructions = blankToNull(req.instructions());
        e.metValue = req.metValue();
    }

    private void validate(ExerciseRequest req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new BadRequestException("El ejercicio necesita un nombre");
        }
        if (req.metValue() != null && req.metValue() < 0) {
            throw new BadRequestException("El MET no puede ser negativo");
        }
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
