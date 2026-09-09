package com.zensyra.kore_domain.external.model;

public record TrainingLoad(
        Double energyConsumption,
        Double avgHeartRate,
        Double maxHeartRate,
        Integer stepCount
) {}