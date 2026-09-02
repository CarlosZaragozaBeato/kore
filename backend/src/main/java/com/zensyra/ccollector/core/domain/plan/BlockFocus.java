package com.zensyra.ccollector.core.domain.plan;

/**
 * Foco de un bloque de periodización. Orienta el tipo de trabajo dominante del
 * bloque; el frontend lo usa además para el color de la banda del calendario.
 */
public enum BlockFocus {
    BASE,
    BUILD,
    PEAK,
    TAPER,
    RECOVERY,
    RACE,
    GENERAL
}
