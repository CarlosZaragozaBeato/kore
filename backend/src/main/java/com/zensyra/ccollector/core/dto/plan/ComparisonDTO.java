package com.zensyra.ccollector.core.dto.plan;

import com.zensyra.ccollector.core.domain.plan.AdherenceBand;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;

import java.time.LocalDate;
import java.util.List;

/**
 * Comparación entre lo planificado para un día y lo realizado (el entreno de
 * Suunto/manual de esa fecha). Es un cálculo derivado, no se persiste: empareja
 * la sesión con el entreno del día y contrasta distancia, duración, ritmo y FC
 * contra los objetivos agregados de sus pasos (Fase D).
 */
public record ComparisonDTO(
        Long plannedSessionId,
        LocalDate date,
        WorkoutType type,
        boolean matched,
        Long workoutId,
        Target target,
        Actual actual,
        List<Metric> metrics
) {

    /** Objetivos agregados de la sesión (de sus pasos o de la propia sesión). */
    public record Target(
            Double distanceMeters,
            Long durationSeconds,
            Integer paceMinSecPerKm,
            Integer paceMaxSecPerKm,
            Integer hrMin,
            Integer hrMax,
            int totalReps
    ) {}

    /** Resumen del entreno realizado emparejado (null si no hubo ninguno ese día). */
    public record Actual(
            Double distanceMeters,
            Long durationSeconds,
            Long paceSecondsPerKm,
            Integer avgHeartRate,
            Integer maxHeartRate
    ) {}

    /**
     * Comparación de una métrica: banda objetivo [low, high], valor real y
     * desviación relativa al centro de la banda. {@code key} ∈ {distance,
     * duration, pace, hr}.
     */
    public record Metric(
            String key,
            Double target,
            Double targetLow,
            Double targetHigh,
            Double actual,
            Double deltaPct,
            AdherenceBand band
    ) {}
}
