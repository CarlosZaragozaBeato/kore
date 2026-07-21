package com.zensyra.ccollector.core.service.suunto;

import com.zensyra.ccollector.core.client.suunto.SuuntoWorkout;
import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.domain.workout.WorkoutSource;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Traduce un entreno de Suunto al modelo interno. Único punto donde viven las
 * suposiciones sobre unidades y tipos de Suunto — ajústalo aquí al validar el
 * esquema real.
 *
 * Supuestos actuales: startTime en epoch-millis; totalDistance en metros;
 * totalTime en segundos; hravg en ppm. El tipo por defecto es RUNNING (uso
 * principal); refinar el mapeo de activityId cuando se confirmen sus valores.
 */
public final class SuuntoWorkoutMapper {

    private SuuntoWorkoutMapper() {
    }

    public static Workout toWorkout(SuuntoWorkout src, Long userId) {
        Workout w = new Workout();
        w.userId = userId;
        w.source = WorkoutSource.SUUNTO;
        w.sourceId = src.workoutKey();
        w.type = mapType(src.activityId());
        w.date = mapDate(src.startTime());
        w.distanceMeters = src.totalDistance();
        w.durationSeconds = src.totalTime() == null ? null : Math.round(src.totalTime());
        w.avgHeartRate = src.hrAvg() == null ? null : (int) Math.round(src.hrAvg());
        w.createdAt = Instant.now();
        return w;
    }

    private static LocalDate mapDate(Long epochMillis) {
        Instant instant = epochMillis == null ? Instant.now() : Instant.ofEpochMilli(epochMillis);
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static WorkoutType mapType(Integer activityId) {
        // TODO: mapear activityId -> tipo cuando se confirmen los valores de Suunto.
        return WorkoutType.RUNNING;
    }
}
