package com.zensyra.ccollector.core.service.context;

import com.zensyra.ccollector.core.domain.auth.CollectorUser;
import com.zensyra.ccollector.core.domain.plan.PlannedSession;
import com.zensyra.ccollector.core.domain.plan.TrainingBlock;
import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.dto.context.TrainingContextDTO;
import com.zensyra.ccollector.core.dto.context.TrainingContextDTO.BlockCtx;
import com.zensyra.ccollector.core.dto.context.TrainingContextDTO.Guidance;
import com.zensyra.ccollector.core.dto.context.TrainingContextDTO.SessionCtx;
import com.zensyra.ccollector.core.dto.context.TrainingContextDTO.StepCtx;
import com.zensyra.ccollector.core.dto.context.TrainingContextDTO.Summary;
import com.zensyra.ccollector.core.dto.context.TrainingContextDTO.WorkoutCtx;
import com.zensyra.ccollector.core.repository.plan.PlannedSessionRepository;
import com.zensyra.ccollector.core.repository.plan.PlannedStepRepository;
import com.zensyra.ccollector.core.repository.plan.TrainingBlockRepository;
import com.zensyra.ccollector.core.repository.workout.WorkoutRepository;
import com.zensyra.ccollector.core.service.analytics.LoadComparisonService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Construye el {@link TrainingContextDTO}: un paquete acotado a un rango de
 * fechas para entregar a un agente. Reúne lo realizado, lo planificado y los
 * bloques que solapan, añade señales de carga y una guía con el formato de
 * vuelta. Solo lectura.
 */
@ApplicationScoped
public class TrainingContextService {

    private static final List<String> WORKOUT_TYPES = List.of("RUNNING", "CYCLING", "SWIMMING", "STRENGTH", "OTHER");
    private static final List<String> STEP_KINDS = List.of("WARMUP", "INTERVAL", "RECOVERY", "STEADY", "COOLDOWN");

    private final WorkoutRepository workouts;
    private final PlannedSessionRepository sessions;
    private final PlannedStepRepository steps;
    private final TrainingBlockRepository blocks;
    private final LoadComparisonService load;

    public TrainingContextService(WorkoutRepository workouts, PlannedSessionRepository sessions,
                                  PlannedStepRepository steps, TrainingBlockRepository blocks,
                                  LoadComparisonService load) {
        this.workouts = workouts;
        this.sessions = sessions;
        this.steps = steps;
        this.blocks = blocks;
        this.load = load;
    }

    public TrainingContextDTO build(CollectorUser user, LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("Indica el rango de fechas (from, to) en formato YYYY-MM-DD");
        }
        if (to.isBefore(from)) {
            throw new BadRequestException("La fecha 'to' no puede ser anterior a 'from'");
        }

        List<WorkoutCtx> doneCtx = workouts.listByUserBetween(user.id, from, to).stream()
                .sorted(Comparator.comparing((Workout w) -> w.date))
                .map(this::toWorkoutCtx)
                .toList();
        List<SessionCtx> plannedCtx = sessions.listByUserBetween(user.id, from, to).stream()
                .sorted(Comparator.comparing((PlannedSession s) -> s.date))
                .map(this::toSessionCtx)
                .toList();
        List<BlockCtx> blockCtx = blocks.listByUser(user.id).stream()
                .filter(b -> !b.startDate.isAfter(to) && !b.endDate.isBefore(from))
                .sorted(Comparator.comparing((TrainingBlock b) -> b.startDate))
                .map(this::toBlockCtx)
                .toList();

        double totalDistance = doneCtx.stream()
                .filter(w -> w.distanceMeters() != null)
                .mapToDouble(WorkoutCtx::distanceMeters).sum();
        long totalDuration = doneCtx.stream()
                .filter(w -> w.durationSeconds() != null)
                .mapToLong(WorkoutCtx::durationSeconds).sum();

        Summary summary = new Summary(doneCtx.size(), totalDistance, totalDuration,
                plannedCtx.size(), load.signals(user.id));

        return new TrainingContextDTO(
                TrainingContextDTO.CURRENT_SCHEMA_VERSION,
                Instant.now(),
                user.username,
                from,
                to,
                guidance(),
                summary,
                doneCtx,
                plannedCtx,
                blockCtx);
    }

    private Guidance guidance() {
        return new Guidance(
                "Contexto de entrenamiento del atleta para un rango de fechas. Úsalo para recomendar "
                        + "las sesiones de los próximos días/semana respetando su carga actual "
                        + "(acwr/monotony/ramp) y los bloques de periodización activos.",
                "Distancias en metros, duraciones en segundos, ritmo en segundos por km, FC en ppm, "
                        + "fechas en ISO YYYY-MM-DD.",
                "Devuelve las sesiones recomendadas como un ARRAY JSON e impórtalo con "
                        + "POST /api/v1/kdl/import/planned (header X-CCollector-Username). Cada sesión: "
                        + "{ date, type, description, status:\"PROPOSED\", steps:[ { kind, repeat, "
                        + "targetDistanceMeters | targetDurationSeconds, targetPaceMinSecPerKm, "
                        + "targetPaceMaxSecPerKm, targetHrMin, targetHrMax, recoverySeconds, note } ] }. "
                        + "Para ofrecer alternativas de un mismo día, dales el mismo variantGroup y "
                        + "distinto variantLabel.",
                WORKOUT_TYPES,
                STEP_KINDS);
    }

    private WorkoutCtx toWorkoutCtx(Workout w) {
        return new WorkoutCtx(w.date, w.type, w.distanceMeters, w.durationSeconds,
                pace(w.distanceMeters, w.durationSeconds), w.avgHeartRate, w.maxHeartRate,
                w.energyKcal, w.perceivedEffort, w.source, w.notes);
    }

    private SessionCtx toSessionCtx(PlannedSession s) {
        List<StepCtx> stepCtx = steps.listBySession(s.id).stream()
                .map(st -> new StepCtx(st.kind, st.repeat, st.targetDistanceMeters, st.targetDurationSeconds,
                        st.targetPaceMinSecPerKm, st.targetPaceMaxSecPerKm, st.targetHrMin, st.targetHrMax,
                        st.recoverySeconds, st.note))
                .toList();
        return new SessionCtx(s.date, s.type, s.targetDistanceMeters, s.targetDurationSeconds,
                s.description, s.status, stepCtx);
    }

    private BlockCtx toBlockCtx(TrainingBlock b) {
        return new BlockCtx(b.level, b.focus, b.name, b.startDate, b.endDate, b.loadStance, b.note);
    }

    /** Ritmo en segundos/km, o null si falta distancia o duración. */
    private Long pace(Double distanceMeters, Long durationSeconds) {
        if (distanceMeters == null || durationSeconds == null || distanceMeters <= 0) {
            return null;
        }
        return Math.round(durationSeconds / (distanceMeters / 1000.0));
    }
}
