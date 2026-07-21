package com.zensyra.ccollector.core.service.gym;

import com.zensyra.ccollector.core.domain.gym.Routine;
import com.zensyra.ccollector.core.domain.gym.RoutineItem;
import com.zensyra.ccollector.core.dto.gym.RoutineDTO;
import com.zensyra.ccollector.core.dto.gym.RoutineDTO.RoutineItemDTO;
import com.zensyra.ccollector.core.dto.gym.RoutineRequest;
import com.zensyra.ccollector.core.repository.gym.RoutineItemRepository;
import com.zensyra.ccollector.core.repository.gym.RoutineRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class RoutineService {

    private final RoutineRepository routines;
    private final RoutineItemRepository items;

    public RoutineService(RoutineRepository routines, RoutineItemRepository items) {
        this.routines = routines;
        this.items = items;
    }

    public List<RoutineDTO> list(Long userId) {
        return routines.listByUser(userId).stream().map(this::toDTO).toList();
    }

    public RoutineDTO get(Long userId, Long id) {
        return toDTO(require(userId, id));
    }

    @Transactional
    public RoutineDTO create(Long userId, RoutineRequest req) {
        validate(req);
        Routine r = new Routine();
        r.userId = userId;
        r.createdAt = Instant.now();
        r.name = req.name().trim();
        r.description = blankToNull(req.description());
        routines.persist(r);
        replaceItems(r.id, req);
        return toDTO(r);
    }

    @Transactional
    public RoutineDTO update(Long userId, Long id, RoutineRequest req) {
        validate(req);
        Routine r = require(userId, id);
        r.name = req.name().trim();
        r.description = blankToNull(req.description());
        replaceItems(r.id, req);
        return toDTO(r);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Routine r = require(userId, id);
        items.deleteByRoutine(r.id);
        routines.delete(r);
    }

    private Routine require(Long userId, Long id) {
        return routines.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Rutina no encontrada"));
    }

    private void replaceItems(Long routineId, RoutineRequest req) {
        items.deleteByRoutine(routineId);
        if (req.items() == null) {
            return;
        }
        int position = 0;
        for (RoutineRequest.ItemRequest ir : req.items()) {
            if (ir.exerciseName() == null || ir.exerciseName().isBlank()) {
                throw new BadRequestException("Cada ejercicio de la rutina necesita un nombre");
            }
            RoutineItem item = new RoutineItem();
            item.routineId = routineId;
            item.position = position++;
            item.exerciseName = ir.exerciseName().trim();
            item.sets = ir.sets();
            item.reps = ir.reps();
            item.restSeconds = ir.restSeconds();
            item.notes = blankToNull(ir.notes());
            items.persist(item);
        }
    }

    private void validate(RoutineRequest req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new BadRequestException("La rutina necesita un nombre");
        }
    }

    private RoutineDTO toDTO(Routine r) {
        List<RoutineItemDTO> itemDTOs = items.listByRoutine(r.id).stream()
                .map(i -> new RoutineItemDTO(i.exerciseName, i.sets, i.reps, i.restSeconds, i.notes))
                .toList();
        return new RoutineDTO(r.id, r.name, r.description, r.createdAt, itemDTOs);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
