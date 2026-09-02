package com.zensyra.ccollector.core.service.suunto;

import com.zensyra.ccollector.core.domain.workout.WorkoutSource;
import com.zensyra.ccollector.core.dto.suunto.PurgeResultDTO;
import com.zensyra.ccollector.core.repository.workout.WorkoutRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Retención mínima de datos de Suunto (Principio 3 del ROADMAP v3). Los entrenos
 * de Suunto son recuperables desde su nube, así que no los guardamos
 * indefinidamente: conservamos solo el mes actual y {@code retentionMonths}
 * meses anteriores. Lo que la app genera (entrenos MANUAL, planes, etc.) no se
 * toca nunca.
 */
@ApplicationScoped
public class SuuntoRetentionService {

    private final WorkoutRepository workouts;
    private final int retentionMonths;
    private final Clock clock;

    @Inject
    public SuuntoRetentionService(
            WorkoutRepository workouts,
            @ConfigProperty(name = "kore.suunto.retention-months", defaultValue = "1") int retentionMonths) {
        this(workouts, retentionMonths, Clock.systemDefaultZone());
    }

    // Para tests con reloj fijo.
    SuuntoRetentionService(WorkoutRepository workouts, int retentionMonths, Clock clock) {
        this.workouts = workouts;
        this.retentionMonths = Math.max(0, retentionMonths);
        this.clock = clock;
    }

    /** Primer día que se conserva: inicio del mes, menos los meses de retención. */
    public LocalDate cutoff() {
        return LocalDate.now(clock).withDayOfMonth(1).minusMonths(retentionMonths);
    }

    @Transactional
    public PurgeResultDTO purge(Long userId) {
        LocalDate cutoff = cutoff();
        long purged = workouts.deleteBySourceBefore(userId, WorkoutSource.SUUNTO, cutoff);
        return new PurgeResultDTO(purged, cutoff);
    }
}
