package com.zensyra.domain.workout.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Embeddable;

@Embeddable
public class WorkoutSummaryMetrics {

    @Column(name = "pte")
    public Double pte;

    @Column(name = "feeling")
    public Integer feeling;

    @Column(name = "peak_epoc")
    public Double peakEpoc;

    @Column(name = "avg_temperature")
    public Double avgTemperature;

    @Column(name = "min_temperature")
    public Double minTemperature;

    @Column(name = "max_temperature")
    public Double maxTemperature;

    @Column(name = "avg_ground_contact_time")
    public Double avgGroundContactTime;

    @Column(name = "avg_vertical_oscillation")
    public Double avgVerticalOscillation;

    @Embedded
    public HeartRateRecovery heartRateRecovery;
}