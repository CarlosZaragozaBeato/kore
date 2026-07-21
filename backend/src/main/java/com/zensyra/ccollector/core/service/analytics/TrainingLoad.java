package com.zensyra.ccollector.core.service.analytics;

import com.zensyra.ccollector.core.domain.workout.Workout;

/**
 * Carga de entrenamiento por sesión (sRPE): duración en minutos × esfuerzo
 * percibido. Cuando no hay RPE (p. ej. entrenos importados de Suunto) se usa un
 * RPE estimado para que el entreno siga contribuyendo. Único punto donde vive la
 * fórmula: ajústalo aquí si cambias el modelo (p. ej. TRIMP por FC).
 */
public final class TrainingLoad {

    /** RPE por defecto cuando el entreno no lo registra (escala 1-10). */
    static final int DEFAULT_RPE = 5;

    private TrainingLoad() {
    }

    public static double of(Workout w) {
        if (w.durationSeconds == null || w.durationSeconds <= 0) {
            return 0;
        }
        double minutes = w.durationSeconds / 60.0;
        int rpe = w.perceivedEffort != null ? w.perceivedEffort : DEFAULT_RPE;
        return minutes * rpe;
    }
}
