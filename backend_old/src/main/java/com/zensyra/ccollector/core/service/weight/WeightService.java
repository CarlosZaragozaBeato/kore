package com.zensyra.ccollector.core.service.weight;

import com.zensyra.ccollector.core.domain.weight.WeightEntry;
import com.zensyra.ccollector.core.domain.weight.WeightGoal;
import com.zensyra.ccollector.core.dto.weight.WeightEntryRequest;
import com.zensyra.ccollector.core.dto.weight.WeightGoalRequest;
import com.zensyra.ccollector.core.repository.weight.WeightEntryRepository;
import com.zensyra.ccollector.core.repository.weight.WeightGoalRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class WeightService {

    private final WeightEntryRepository entries;
    private final WeightGoalRepository goals;

    public WeightService(WeightEntryRepository entries, WeightGoalRepository goals) {
        this.entries = entries;
        this.goals = goals;
    }

    public List<WeightEntry> list(Long userId) {
        return entries.listByUser(userId);
    }

    /** Alta o actualización de la medición de un día (un registro por fecha). */
    @Transactional
    public WeightEntry upsert(Long userId, WeightEntryRequest req) {
        validate(req);
        WeightEntry e = entries.findByDateAndUser(req.date(), userId).orElseGet(WeightEntry::new);
        e.userId = userId;
        e.date = req.date();
        e.weightKg = req.weightKg();
        if (e.id == null) {
            entries.persist(e);
        }
        return e;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        entries.delete(entries.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Medición no encontrada")));
    }

    public WeightGoal goal(Long userId) {
        return goals.findByUser(userId).orElse(null);
    }

    @Transactional
    public WeightGoal setGoal(Long userId, WeightGoalRequest req) {
        if (req != null && req.minKg() != null && req.maxKg() != null && req.minKg() > req.maxKg()) {
            throw new BadRequestException("El peso mínimo no puede ser mayor que el máximo");
        }
        WeightGoal g = goals.findByUser(userId).orElseGet(WeightGoal::new);
        g.userId = userId;
        g.minKg = req == null ? null : req.minKg();
        g.maxKg = req == null ? null : req.maxKg();
        g.maintenanceKcal = req == null ? null : req.maintenanceKcal();
        if (g.id == null) {
            goals.persist(g);
        }
        return g;
    }

    private void validate(WeightEntryRequest req) {
        if (req == null || req.date() == null) {
            throw new BadRequestException("La fecha de la medición es obligatoria");
        }
        if (req.date().isAfter(LocalDate.now())) {
            throw new BadRequestException("La fecha no puede ser futura");
        }
        if (req.weightKg() == null || req.weightKg() <= 0) {
            throw new BadRequestException("El peso debe ser mayor que cero");
        }
    }
}
