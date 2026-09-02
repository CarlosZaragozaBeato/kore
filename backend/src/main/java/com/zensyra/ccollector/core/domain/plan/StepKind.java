package com.zensyra.ccollector.core.domain.plan;

/**
 * Tipo de paso dentro de una sesión planificada estructurada.
 * WARMUP: calentamiento; INTERVAL: serie/repetición de esfuerzo; RECOVERY:
 * recuperación entre esfuerzos; STEADY: rodaje/continuo; COOLDOWN: vuelta a la
 * calma.
 */
public enum StepKind {
    WARMUP,
    INTERVAL,
    RECOVERY,
    STEADY,
    COOLDOWN
}
