package com.zensyra.ccollector.core.domain.plan;

/**
 * Estado de una sesión planificada en el calendario.
 * PROPOSED: propuesta/recomendación pendiente de decisión; ACCEPTED: aceptada
 * (la que se hará); REJECTED: descartada. Las sesiones dentro de un plan nacen
 * ACCEPTED.
 */
public enum SessionStatus {
    PROPOSED,
    ACCEPTED,
    REJECTED
}
