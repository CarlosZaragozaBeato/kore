package com.zensyra.ccollector.core.service.activity;

import com.zensyra.ccollector.core.domain.activity.DailyActivity;
import com.zensyra.ccollector.core.dto.activity.DailyActivityRequest;
import com.zensyra.ccollector.core.repository.activity.DailyActivityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class DailyActivityService {

    private final DailyActivityRepository activity;

    public DailyActivityService(DailyActivityRepository activity) {
        this.activity = activity;
    }

    public List<DailyActivity> list(Long userId) {
        return activity.listByUser(userId);
    }

    /** Alta o actualización de la actividad de un día (un registro por fecha). */
    @Transactional
    public DailyActivity upsert(Long userId, DailyActivityRequest req) {
        validate(req);
        DailyActivity a = activity.findByDateAndUser(req.date(), userId).orElseGet(DailyActivity::new);
        a.userId = userId;
        a.date = req.date();
        a.steps = req.steps();
        a.burnedKcal = req.burnedKcal();
        if (a.id == null) {
            activity.persist(a);
        }
        return a;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        activity.delete(activity.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Actividad no encontrada")));
    }

    private void validate(DailyActivityRequest req) {
        if (req == null || req.date() == null) {
            throw new BadRequestException("La fecha de la actividad es obligatoria");
        }
        if (req.date().isAfter(LocalDate.now())) {
            throw new BadRequestException("La fecha no puede ser futura");
        }
        if (req.steps() != null && req.steps() < 0) {
            throw new BadRequestException("Los pasos no pueden ser negativos");
        }
        if (req.burnedKcal() != null && req.burnedKcal() < 0) {
            throw new BadRequestException("La quema no puede ser negativa");
        }
    }
}
