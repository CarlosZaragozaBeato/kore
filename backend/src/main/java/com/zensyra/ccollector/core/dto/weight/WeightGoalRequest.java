package com.zensyra.ccollector.core.dto.weight;

/** Fija el objetivo de mantenimiento (rango de peso y kcal/día). */
public record WeightGoalRequest(Double minKg, Double maxKg, Double maintenanceKcal) {
}
