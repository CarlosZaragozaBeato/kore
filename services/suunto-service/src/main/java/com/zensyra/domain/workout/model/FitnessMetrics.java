package com.zensyra.domain.workout.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class FitnessMetrics {

    @Column(name = "vo2_max")
    public Double vo2Max;

    @Column(name = "estimated_vo2_max")
    public Double estimatedVo2Max;

    @Column(name = "fitness_age")
    public Integer fitnessAge;
}