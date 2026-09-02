package com.zensyra.ccollector.core.domain.plan;

/**
 * Postura de carga recomendada para los próximos días, derivada de las señales
 * de control de carga (ACWR/monotonía/rampa): bajar, mantener o subir.
 */
public enum LoadStance {
    DELOAD,
    MAINTAIN,
    BUILD
}
