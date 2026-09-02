package com.zensyra.ccollector.core.service.plan;

import com.zensyra.ccollector.core.domain.plan.AdherenceBand;
import com.zensyra.ccollector.core.domain.plan.PlannedSession;
import com.zensyra.ccollector.core.domain.plan.PlannedStep;
import com.zensyra.ccollector.core.domain.plan.StepKind;
import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;
import com.zensyra.ccollector.core.dto.plan.ComparisonDTO;
import com.zensyra.ccollector.core.dto.plan.ComparisonDTO.Actual;
import com.zensyra.ccollector.core.dto.plan.ComparisonDTO.Metric;
import com.zensyra.ccollector.core.dto.plan.ComparisonDTO.Target;
import com.zensyra.ccollector.core.repository.plan.PlannedSessionRepository;
import com.zensyra.ccollector.core.repository.plan.PlannedStepRepository;
import com.zensyra.ccollector.core.repository.workout.WorkoutRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Comparación planificado vs realizado (Fase D). Empareja la sesión planificada
 * de un día con el entreno registrado (Suunto o manual) de esa fecha y contrasta
 * las métricas de resumen contra los objetivos agregados de los pasos. Trabaja a
 * nivel de sesión: el entreno guardado es un resumen (sin splits por vuelta), así
 * que no se comparan repeticiones individuales, sólo totales y medias.
 */
@ApplicationScoped
public class ComparisonService {

    /** Tolerancia para dar por cumplida una distancia (±5 %). */
    private static final double DISTANCE_TOL = 0.05;
    /** Tolerancia para dar por cumplida una duración (±10 %). */
    private static final double DURATION_TOL = 0.10;

    private final PlannedSessionRepository sessions;
    private final PlannedStepRepository steps;
    private final WorkoutRepository workouts;

    public ComparisonService(PlannedSessionRepository sessions, PlannedStepRepository steps,
                             WorkoutRepository workouts) {
        this.sessions = sessions;
        this.steps = steps;
        this.workouts = workouts;
    }

    public ComparisonDTO compare(Long userId, Long sessionId) {
        PlannedSession s = sessions.findByIdAndUser(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Sesión planificada no encontrada"));
        Target target = aggregate(s, steps.listBySession(s.id));
        Workout w = pickWorkout(userId, s.date, s.type, target.distanceMeters());
        Actual actual = w == null ? null : new Actual(
                w.distanceMeters, w.durationSeconds, pace(w.distanceMeters, w.durationSeconds),
                w.avgHeartRate, w.maxHeartRate);
        return new ComparisonDTO(
                s.id, s.date, s.type, w != null, w == null ? null : w.id,
                target, actual, buildMetrics(target, actual));
    }

    /** Agrega los objetivos: los de la sesión mandan; si no, se suman los de los pasos. */
    private Target aggregate(PlannedSession s, List<PlannedStep> stepList) {
        Double distance = s.targetDistanceMeters;
        if (distance == null) {
            double sum = 0;
            boolean any = false;
            for (PlannedStep st : stepList) {
                if (st.targetDistanceMeters != null) {
                    sum += st.repeat * st.targetDistanceMeters;
                    any = true;
                }
            }
            if (any) {
                distance = sum;
            }
        }

        Long duration = s.targetDurationSeconds;
        if (duration == null && !stepList.isEmpty()
                && stepList.stream().allMatch(st -> st.targetDurationSeconds != null)) {
            long sum = 0;
            for (PlannedStep st : stepList) {
                sum += (long) st.repeat * st.targetDurationSeconds;
                if (st.recoverySeconds != null) {
                    sum += (long) st.repeat * st.recoverySeconds;
                }
            }
            duration = sum;
        }

        Integer paceMin = null;
        Integer paceMax = null;
        Integer hrMin = null;
        Integer hrMax = null;
        for (PlannedStep st : stepList) {
            paceMin = min(paceMin, st.targetPaceMinSecPerKm);
            paceMax = max(paceMax, st.targetPaceMaxSecPerKm);
            hrMin = min(hrMin, st.targetHrMin);
            hrMax = max(hrMax, st.targetHrMax);
        }
        // Si sólo hay un extremo, la banda se colapsa a un único valor.
        if (paceMin == null) {
            paceMin = paceMax;
        }
        if (paceMax == null) {
            paceMax = paceMin;
        }
        if (hrMin == null) {
            hrMin = hrMax;
        }
        if (hrMax == null) {
            hrMax = hrMin;
        }

        int reps = stepList.stream().filter(st -> st.kind == StepKind.INTERVAL)
                .mapToInt(st -> st.repeat).sum();
        return new Target(distance, duration, paceMin, paceMax, hrMin, hrMax, reps);
    }

    /** Elige el entreno del día que mejor casa: mismo tipo y distancia más cercana. */
    private Workout pickWorkout(Long userId, LocalDate date, WorkoutType type, Double targetDistance) {
        List<Workout> onDay = workouts.listByUserAndDate(userId, date);
        if (onDay.isEmpty()) {
            return null;
        }
        List<Workout> typed = onDay.stream().filter(w -> w.type == type).toList();
        List<Workout> pool = typed.isEmpty() ? onDay : typed;
        if (targetDistance != null) {
            return pool.stream()
                    .filter(w -> w.distanceMeters != null)
                    .min(Comparator.comparingDouble(w -> Math.abs(w.distanceMeters - targetDistance)))
                    .orElse(pool.get(0));
        }
        return pool.get(0);
    }

    private List<Metric> buildMetrics(Target t, Actual a) {
        List<Metric> out = new ArrayList<>();
        if (t.distanceMeters() != null) {
            out.add(single("distance", t.distanceMeters(),
                    a == null ? null : a.distanceMeters(), DISTANCE_TOL));
        }
        if (t.durationSeconds() != null) {
            out.add(single("duration", (double) t.durationSeconds(),
                    a == null || a.durationSeconds() == null ? null : (double) a.durationSeconds(),
                    DURATION_TOL));
        }
        if (t.paceMinSecPerKm() != null && t.paceMaxSecPerKm() != null) {
            out.add(range("pace", t.paceMinSecPerKm(), t.paceMaxSecPerKm(),
                    a == null || a.paceSecondsPerKm() == null ? null : (double) a.paceSecondsPerKm()));
        }
        if (t.hrMin() != null && t.hrMax() != null) {
            out.add(range("hr", t.hrMin(), t.hrMax(),
                    a == null || a.avgHeartRate() == null ? null : (double) a.avgHeartRate()));
        }
        return out;
    }

    /** Métrica con objetivo único (distancia/duración): banda = objetivo ± tolerancia. */
    private Metric single(String key, double target, Double actual, double tol) {
        double low = target * (1 - tol);
        double high = target * (1 + tol);
        if (actual == null) {
            return new Metric(key, target, low, high, null, null, AdherenceBand.NO_DATA);
        }
        return new Metric(key, target, low, high, actual,
                round1((actual - target) / target * 100.0), band(actual, low, high));
    }

    /** Métrica con banda objetivo (ritmo/FC): desviación relativa al centro. */
    private Metric range(String key, double low, double high, Double actual) {
        double mid = (low + high) / 2.0;
        if (actual == null) {
            return new Metric(key, mid, low, high, null, null, AdherenceBand.NO_DATA);
        }
        return new Metric(key, mid, low, high, actual,
                round1((actual - mid) / mid * 100.0), band(actual, low, high));
    }

    private AdherenceBand band(double actual, double low, double high) {
        if (actual < low) {
            return AdherenceBand.BELOW;
        }
        if (actual > high) {
            return AdherenceBand.ABOVE;
        }
        return AdherenceBand.WITHIN;
    }

    private static Integer min(Integer a, Integer b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return Math.min(a, b);
    }

    private static Integer max(Integer a, Integer b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return Math.max(a, b);
    }

    private static Long pace(Double meters, Long seconds) {
        if (meters == null || seconds == null || meters <= 0) {
            return null;
        }
        return Math.round(seconds / (meters / 1000.0));
    }

    private static Double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
