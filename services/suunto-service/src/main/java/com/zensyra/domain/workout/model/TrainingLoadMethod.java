package com.zensyra.domain.workout.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TrainingLoadMethod {

    @Column(name = "training_stress_score")
    public Double trainingStressScore;

    @Column(name = "intensity_factor")
    public Double intensityFactor;

    @Column(name = "normalized_power")
    public Double normalizedPower;

    @Column(name = "average_grade_adjusted_pace")
    public Double averageGradeAdjustedPace;
}