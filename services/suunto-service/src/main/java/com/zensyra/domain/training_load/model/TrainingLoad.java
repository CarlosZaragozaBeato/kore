package com.zensyra.domain.training_load.model;

public record TrainingLoad(
        Double energyConsumption,
        Double avgHeartRate,
        Double maxHeartRate,
        Integer stepCount
) {
}