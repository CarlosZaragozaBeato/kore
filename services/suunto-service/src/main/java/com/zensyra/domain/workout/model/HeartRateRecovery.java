package com.zensyra.domain.workout.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class HeartRateRecovery {

    @Column(name = "hr_recovery_drop")
    public Double drop;

    @Column(name = "hr_recovery_level")
    public String level;

    @Column(name = "hr_recovery_comparison_level")
    public String comparisonLevel;
}