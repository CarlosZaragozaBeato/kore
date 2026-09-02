package com.zensyra.ccollector.core.service.suunto;

import com.zensyra.ccollector.core.client.suunto.SuuntoWorkout;
import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.domain.workout.WorkoutSource;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

/**
 * Traduce un entreno de Suunto al modelo interno. Único punto donde viven las
 * suposiciones sobre unidades y tipos de Suunto.
 *
 * Unidades (validadas contra datos reales): startTime en epoch-millis;
 * totalDistance en metros; totalTime en segundos; energyConsumption en kcal;
 * FC (anidada en hrdata) en ppm.
 *
 * El tipo se deriva del {@code activityId} de Suunto con la tabla oficial de
 * IDs (docs/brand aparte; ver docs/AGENTS.md). Solo distinguimos las
 * disciplinas que la app pinta con color propio (carrera/ciclismo/natación) más
 * fuerza; el resto cae en OTHER.
 */
public final class SuuntoWorkoutMapper {

    private SuuntoWorkoutMapper() {
    }

    // activityId (Support ID de Suunto) -> disciplina interna. IDs no listados
    // (senderismo, esquí, remo, deportes de pelota, etc.) -> OTHER.
    private static final Set<Integer> RUNNING_IDS = Set.of(
            1,   // Running
            22,  // Trail running
            53,  // Treadmill
            59,  // Track and field
            60,  // Orienteering
            103, // Track running
            115  // Vertical running
    );
    private static final Set<Integer> CYCLING_IDS = Set.of(
            2,   // Cycling
            10,  // Mountain biking
            52,  // Indoor cycling
            99,  // Gravel cycling
            105, // E-biking
            106, // E-mtb
            109, // Hand cycling
            114  // Cyclocross
    );
    private static final Set<Integer> SWIMMING_IDS = Set.of(
            21,  // Swimming
            85,  // Openwater swimming
            90   // Snorkeling
    );
    private static final Set<Integer> STRENGTH_IDS = Set.of(
            20,  // Outdoor gym
            23,  // Gym
            54,  // Crossfit
            63,  // Kettlebell
            104  // Calisthenics
    );

    private static final Map<Integer, WorkoutType> TYPE_BY_ACTIVITY = build();

    private static Map<Integer, WorkoutType> build() {
        var m = new java.util.HashMap<Integer, WorkoutType>();
        RUNNING_IDS.forEach(id -> m.put(id, WorkoutType.RUNNING));
        CYCLING_IDS.forEach(id -> m.put(id, WorkoutType.CYCLING));
        SWIMMING_IDS.forEach(id -> m.put(id, WorkoutType.SWIMMING));
        STRENGTH_IDS.forEach(id -> m.put(id, WorkoutType.STRENGTH));
        return Map.copyOf(m);
    }

    public static Workout toWorkout(SuuntoWorkout src, Long userId) {
        Workout w = new Workout();
        w.userId = userId;
        w.source = WorkoutSource.SUUNTO;
        w.sourceId = src.workoutKey();
        w.createdAt = Instant.now();
        applyMetrics(w, src);
        return w;
    }

    /**
     * Vuelca las métricas de Suunto sobre un entreno existente (re-sync/backfill),
     * sin tocar id, userId, source, sourceId ni createdAt. Con esto un re-sync
     * rellena entrenos ya importados con FC/tipo/kcal/pasos que faltaban.
     */
    public static void applyMetrics(Workout w, SuuntoWorkout src) {
        w.type = mapType(src.activityId());
        w.date = mapDate(src.startTime());
        w.distanceMeters = src.totalDistance();
        w.durationSeconds = src.totalTime() == null ? null : Math.round(src.totalTime());
        w.avgHeartRate = roundHr(src.avgHeartRate());
        w.maxHeartRate = roundHr(src.maxHeartRate());
        w.energyKcal = src.energyConsumption();
        w.stepCount = src.stepCount();
    }

    private static Integer roundHr(Double hr) {
        return hr == null ? null : (int) Math.round(hr);
    }

    private static LocalDate mapDate(Long epochMillis) {
        Instant instant = epochMillis == null ? Instant.now() : Instant.ofEpochMilli(epochMillis);
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    static WorkoutType mapType(Integer activityId) {
        if (activityId == null) {
            return WorkoutType.OTHER;
        }
        return TYPE_BY_ACTIVITY.getOrDefault(activityId, WorkoutType.OTHER);
    }
}
