package com.zensyra.ccollector.core.domain.workout;

/**
 * Tipo/disciplina de entrenamiento. Las tres disciplinas del triatlón
 * (RUNNING/CYCLING/SWIMMING) tienen color propio en la UI (ver
 * {@code frontend/src/discipline.ts}). STRENGTH agrupa gimnasio/fuerza; OTHER
 * el resto. El mapeo desde el {@code activityId} de Suunto vive en
 * {@code SuuntoWorkoutMapper}.
 */
public enum WorkoutType {
    RUNNING,
    CYCLING,
    SWIMMING,
    STRENGTH,
    OTHER
}
