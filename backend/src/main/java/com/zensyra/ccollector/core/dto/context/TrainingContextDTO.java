package com.zensyra.ccollector.core.dto.context;

import com.zensyra.ccollector.core.domain.plan.BlockFocus;
import com.zensyra.ccollector.core.domain.plan.BlockLevel;
import com.zensyra.ccollector.core.domain.plan.LoadStance;
import com.zensyra.ccollector.core.domain.plan.SessionStatus;
import com.zensyra.ccollector.core.domain.plan.StepKind;
import com.zensyra.ccollector.core.domain.workout.WorkoutSource;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;
import com.zensyra.ccollector.core.dto.analytics.LoadSignalsDTO;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Contexto de entrenamiento acotado a un rango de fechas, pensado para
 * <b>entregárselo a un agente</b> y que devuelva recomendaciones de sesiones.
 * Es autodescriptivo: incluye lo realizado, lo planificado, los bloques de
 * periodización que solapan, señales de carga y una guía ({@link Guidance}) con
 * unidades y el formato exacto para importar el plan de vuelta. No incluye
 * ningún secreto ni id de base de datos.
 */
public record TrainingContextDTO(
        int schemaVersion,
        Instant generatedAt,
        String username,
        LocalDate from,
        LocalDate to,
        Guidance guidance,
        Summary summary,
        List<WorkoutCtx> workouts,
        List<SessionCtx> planned,
        List<BlockCtx> blocks
) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    /** Instrucciones para que el agente entienda los datos y sepa devolver el plan. */
    public record Guidance(
            String purpose,
            String units,
            String returnFormat,
            List<String> workoutTypes,
            List<String> stepKinds
    ) {
    }

    public record Summary(
            int workoutCount,
            double totalDistanceMeters,
            long totalDurationSeconds,
            int plannedCount,
            LoadSignalsDTO load
    ) {
    }

    public record WorkoutCtx(
            LocalDate date,
            WorkoutType type,
            Double distanceMeters,
            Long durationSeconds,
            Long paceSecondsPerKm,
            Integer avgHeartRate,
            Integer maxHeartRate,
            Double energyKcal,
            Integer perceivedEffort,
            WorkoutSource source,
            String notes
    ) {
    }

    public record SessionCtx(
            LocalDate date,
            WorkoutType type,
            Double targetDistanceMeters,
            Long targetDurationSeconds,
            String description,
            SessionStatus status,
            List<StepCtx> steps
    ) {
    }

    public record StepCtx(
            StepKind kind,
            int repeat,
            Double targetDistanceMeters,
            Long targetDurationSeconds,
            Integer targetPaceMinSecPerKm,
            Integer targetPaceMaxSecPerKm,
            Integer targetHrMin,
            Integer targetHrMax,
            Long recoverySeconds,
            String note
    ) {
    }

    public record BlockCtx(
            BlockLevel level,
            BlockFocus focus,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            LoadStance loadStance,
            String note
    ) {
    }
}
