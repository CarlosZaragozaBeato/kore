package com.zensyra.ccollector.core.domain.weight;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Objetivo de peso del atleta. Pensado para MANTENIMIENTO: un rango
 * [minKg, maxKg] en lugar de un único número, más un objetivo diario de kcal
 * ({@link #maintenanceKcal}) contra el que evaluar el balance energético.
 * Uno por usuario.
 */
@Entity
@Table(name = "weight_goals")
public class WeightGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    public Long userId;

    @Column(name = "min_kg")
    public Double minKg;

    @Column(name = "max_kg")
    public Double maxKg;

    /** kcal/día de mantenimiento (gasto total estimado). Opcional. */
    @Column(name = "maintenance_kcal")
    public Double maintenanceKcal;
}
