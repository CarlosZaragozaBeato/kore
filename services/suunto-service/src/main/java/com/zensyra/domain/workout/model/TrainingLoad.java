package com.zensyra.domain.workout.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Embeddable;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;

@Embeddable
public class TrainingLoad {

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "trainingStressScore",
                    column = @Column(name = "tss_hr")
            ),
            @AttributeOverride(
                    name = "intensityFactor",
                    column = @Column(name = "tss_hr_intensity_factor")
            ),
            @AttributeOverride(
                    name = "normalizedPower",
                    column = @Column(name = "tss_hr_normalized_power")
            ),
            @AttributeOverride(
                    name = "averageGradeAdjustedPace",
                    column = @Column(name = "tss_hr_avg_grade_adjusted_pace")
            )
    })
    public TrainingLoadMethod hr;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "trainingStressScore",
                    column = @Column(name = "tss_power")
            ),
            @AttributeOverride(
                    name = "intensityFactor",
                    column = @Column(name = "tss_power_intensity_factor")
            ),
            @AttributeOverride(
                    name = "normalizedPower",
                    column = @Column(name = "tss_power_normalized_power")
            ),
            @AttributeOverride(
                    name = "averageGradeAdjustedPace",
                    column = @Column(name = "tss_power_avg_grade_adjusted_pace")
            )
    })
    public TrainingLoadMethod power;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "trainingStressScore",
                    column = @Column(name = "tss_pace")
            ),
            @AttributeOverride(
                    name = "intensityFactor",
                    column = @Column(name = "tss_pace_intensity_factor")
            ),
            @AttributeOverride(
                    name = "normalizedPower",
                    column = @Column(name = "tss_pace_normalized_power")
            ),
            @AttributeOverride(
                    name = "averageGradeAdjustedPace",
                    column = @Column(name = "tss_pace_avg_grade_adjusted_pace")
            )
    })
    public TrainingLoadMethod pace;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "trainingStressScore",
                    column = @Column(name = "tss_met")
            ),
            @AttributeOverride(
                    name = "intensityFactor",
                    column = @Column(name = "tss_met_intensity_factor")
            ),
            @AttributeOverride(
                    name = "normalizedPower",
                    column = @Column(name = "tss_met_normalized_power")
            ),
            @AttributeOverride(
                    name = "averageGradeAdjustedPace",
                    column = @Column(name = "tss_met_avg_grade_adjusted_pace")
            )
    })
    public TrainingLoadMethod met;
}