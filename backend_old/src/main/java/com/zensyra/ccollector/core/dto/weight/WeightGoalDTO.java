package com.zensyra.ccollector.core.dto.weight;

import com.zensyra.ccollector.core.domain.weight.WeightGoal;

/** Objetivo de mantenimiento: rango de peso y kcal/día de referencia. */
public record WeightGoalDTO(Double minKg, Double maxKg, Double maintenanceKcal) {

    public static WeightGoalDTO from(WeightGoal g) {
        return g == null ? new WeightGoalDTO(null, null, null)
                : new WeightGoalDTO(g.minKg, g.maxKg, g.maintenanceKcal);
    }
}
