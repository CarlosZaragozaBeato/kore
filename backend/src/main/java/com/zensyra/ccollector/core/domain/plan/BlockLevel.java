package com.zensyra.ccollector.core.domain.plan;

/**
 * Nivel de un bloque de periodización, de mayor a menor alcance.
 * MACRO = temporada/objetivo (meses/año); MESO = bloque de varias semanas con
 * un foco (base/construcción/pico/afinamiento); MICRO = una semana.
 */
public enum BlockLevel {
    MACRO,
    MESO,
    MICRO
}
