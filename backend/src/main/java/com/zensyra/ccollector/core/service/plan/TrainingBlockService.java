package com.zensyra.ccollector.core.service.plan;

import com.zensyra.ccollector.core.domain.plan.BlockFocus;
import com.zensyra.ccollector.core.domain.plan.PlannedSession;
import com.zensyra.ccollector.core.domain.plan.TrainingBlock;
import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.dto.plan.BlockDTO;
import com.zensyra.ccollector.core.dto.plan.BlockRequest;
import com.zensyra.ccollector.core.dto.plan.BlockSummaryDTO;
import com.zensyra.ccollector.core.repository.plan.PlannedSessionRepository;
import com.zensyra.ccollector.core.repository.plan.TrainingBlockRepository;
import com.zensyra.ccollector.core.repository.workout.WorkoutRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Periodización larga (Fase F): bloques macro/meso/micro sobre el calendario.
 * Los bloques se anidan por {@code parentId}; la relación con las sesiones es
 * derivada por solape de fechas, así que el resumen de un bloque
 * ({@link #summary}) agrega planificado vs realizado sin ninguna clave ajena.
 */
@ApplicationScoped
public class TrainingBlockService {

    private final TrainingBlockRepository blocks;
    private final PlannedSessionRepository sessions;
    private final WorkoutRepository workouts;

    public TrainingBlockService(TrainingBlockRepository blocks, PlannedSessionRepository sessions,
                                WorkoutRepository workouts) {
        this.blocks = blocks;
        this.sessions = sessions;
        this.workouts = workouts;
    }

    /** Árbol de bloques del usuario (raíces con sus hijos anidados). */
    public List<BlockDTO> tree(Long userId) {
        List<TrainingBlock> all = blocks.listByUser(userId);
        Map<Long, List<TrainingBlock>> byParent = all.stream()
                .filter(b -> b.parentId != null)
                .collect(Collectors.groupingBy(b -> b.parentId));
        return all.stream()
                .filter(b -> b.parentId == null)
                .map(b -> toTree(b, byParent))
                .toList();
    }

    private BlockDTO toTree(TrainingBlock b, Map<Long, List<TrainingBlock>> byParent) {
        List<BlockDTO> children = byParent.getOrDefault(b.id, List.of()).stream()
                .map(c -> toTree(c, byParent))
                .toList();
        return BlockDTO.from(b, children);
    }

    @Transactional
    public BlockDTO create(Long userId, BlockRequest req) {
        return createUnder(userId, req.parentId(), req);
    }

    /** Crea un bloque bajo {@code parentId} y, recursivamente, sus hijos anidados. */
    private BlockDTO createUnder(Long userId, Long parentId, BlockRequest req) {
        validate(userId, parentId, req);
        TrainingBlock b = new TrainingBlock();
        b.userId = userId;
        b.parentId = parentId;
        apply(b, req);
        blocks.persist(b);
        List<BlockDTO> children = new ArrayList<>();
        if (req.children() != null) {
            for (BlockRequest child : req.children()) {
                children.add(createUnder(userId, b.id, child));
            }
        }
        return BlockDTO.from(b, children);
    }

    @Transactional
    public BlockDTO update(Long userId, Long id, BlockRequest req) {
        TrainingBlock b = find(userId, id);
        validate(userId, b.parentId, req);
        apply(b, req);
        return BlockDTO.from(b, List.of());
    }

    /** Borra un bloque y, en cascada, todos sus descendientes. */
    @Transactional
    public void delete(Long userId, Long id) {
        TrainingBlock b = find(userId, id);
        deleteRecursive(userId, b);
    }

    private void deleteRecursive(Long userId, TrainingBlock b) {
        for (TrainingBlock child : blocks.listChildren(userId, b.id)) {
            deleteRecursive(userId, child);
        }
        blocks.delete(b);
    }

    /** Resumen derivado del bloque: planificado vs realizado en su rango de fechas. */
    public BlockSummaryDTO summary(Long userId, Long id) {
        TrainingBlock b = find(userId, id);
        List<PlannedSession> planned = sessions.listByUserBetween(userId, b.startDate, b.endDate);
        List<Workout> done = workouts.listByUserBetween(userId, b.startDate, b.endDate);
        double plannedDistance = planned.stream()
                .filter(s -> s.targetDistanceMeters != null)
                .mapToDouble(s -> s.targetDistanceMeters).sum();
        double doneDistance = done.stream()
                .filter(w -> w.distanceMeters != null)
                .mapToDouble(w -> w.distanceMeters).sum();
        int weeks = (int) Math.max(1, Math.ceil((ChronoUnit.DAYS.between(b.startDate, b.endDate) + 1) / 7.0));
        Integer adherence = planned.isEmpty() ? null
                : (int) Math.round(100.0 * Math.min(done.size(), planned.size()) / planned.size());
        return new BlockSummaryDTO(b.id, b.startDate, b.endDate, weeks,
                planned.size(), done.size(), plannedDistance, doneDistance, adherence);
    }

    private TrainingBlock find(Long userId, Long id) {
        return blocks.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Bloque de periodización no encontrado"));
    }

    private void apply(TrainingBlock b, BlockRequest req) {
        b.level = req.level();
        b.focus = req.focus() != null ? req.focus() : BlockFocus.GENERAL;
        b.name = req.name().trim();
        b.startDate = req.startDate();
        b.endDate = req.endDate();
        b.loadStance = req.loadStance();
        b.note = req.note() == null || req.note().isBlank() ? null : req.note().trim();
    }

    private void validate(Long userId, Long parentId, BlockRequest req) {
        if (req == null || req.level() == null) {
            throw new BadRequestException("El bloque necesita un nivel (MACRO/MESO/MICRO)");
        }
        if (req.name() == null || req.name().isBlank()) {
            throw new BadRequestException("El bloque necesita un nombre");
        }
        if (req.startDate() == null || req.endDate() == null) {
            throw new BadRequestException("El bloque necesita fecha de inicio y fin");
        }
        if (req.endDate().isBefore(req.startDate())) {
            throw new BadRequestException("La fecha de fin no puede ser anterior al inicio");
        }
        if (parentId != null) {
            TrainingBlock parent = blocks.findByIdAndUser(parentId, userId)
                    .orElseThrow(() -> new BadRequestException("El bloque padre no existe"));
            if (parent.level.ordinal() >= req.level().ordinal()) {
                throw new BadRequestException(
                        "Un bloque debe tener un nivel más fino que su contenedor (MACRO>MESO>MICRO)");
            }
        }
    }
}
