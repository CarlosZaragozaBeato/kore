package com.zensyra.ccollector.core.domain.gym;

/**
 * Fase/rol del ejercicio dentro de una sesión:
 * <ul>
 *   <li>{@code WARMUP} — calentamiento antes de sesión o recuperación.</li>
 *   <li>{@code STRENGTH} — trabajo principal de fuerza.</li>
 *   <li>{@code RECOVERY} — ejercicios de recuperación.</li>
 * </ul>
 * El uso de material se indica aparte ({@code requiresEquipment} + {@code equipment}).
 */
public enum ExerciseCategory {
    WARMUP,
    STRENGTH,
    RECOVERY
}
