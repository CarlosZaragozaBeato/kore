package com.zensyra.domain.workout.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class IntensityZone {

    @Column(name = "total_time")
    public Double totalTime;

    @Column(name = "lower_limit")
    public Double lowerLimit;
}