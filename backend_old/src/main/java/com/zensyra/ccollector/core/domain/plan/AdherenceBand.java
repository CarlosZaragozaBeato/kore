package com.zensyra.ccollector.core.domain.plan;

/**
 * Dónde cae el valor realizado respecto a la banda objetivo de una métrica.
 * Su significado depende de la métrica: para ritmo, {@code BELOW} es más rápido
 * (menos s/km) y {@code ABOVE} más lento; para distancia/FC, por debajo/encima.
 */
public enum AdherenceBand {
    BELOW,
    WITHIN,
    ABOVE,
    NO_DATA
}
