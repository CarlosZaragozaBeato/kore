package com.zensyra.ccollector.core.service.energy;

import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;

/**
 * Estima las kcal quemadas en un entreno. Si el entreno trae la energía real de
 * Suunto ({@code energyKcal}) se usa esa; si no, se estima por tipo y duración
 * con un factor kcal/min (heurística orientativa, no medición). Único punto
 * donde vive la estimación: ajústalo aquí.
 */
public final class EnergyEstimate {

    private EnergyEstimate() {
    }

    private static double kcalPerMinute(WorkoutType type) {
        if (type == null) {
            return 7.0;
        }
        return switch (type) {
            case RUNNING -> 11.0;
            case CYCLING -> 8.0;
            case SWIMMING -> 9.0;
            case STRENGTH -> 6.0;
            case OTHER -> 7.0;
        };
    }

    public static double burnedKcal(Workout w) {
        if (w.energyKcal != null && w.energyKcal > 0) {
            return w.energyKcal;
        }
        if (w.durationSeconds == null || w.durationSeconds <= 0) {
            return 0;
        }
        return (w.durationSeconds / 60.0) * kcalPerMinute(w.type);
    }
}
