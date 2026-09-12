package com.zensyra.domain.workout.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Embeddable;

@Embeddable
public class IntensityZones {

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "totalTime",
                    column = @Column(name = "hr_zone1_total_time")
            ),
            @AttributeOverride(
                    name = "lowerLimit",
                    column = @Column(name = "hr_zone1_lower_limit")
            )
    })
    public IntensityZone heartRateZone1;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "totalTime",
                    column = @Column(name = "hr_zone2_total_time")
            ),
            @AttributeOverride(
                    name = "lowerLimit",
                    column = @Column(name = "hr_zone2_lower_limit")
            )
    })
    public IntensityZone heartRateZone2;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "totalTime",
                    column = @Column(name = "hr_zone3_total_time")
            ),
            @AttributeOverride(
                    name = "lowerLimit",
                    column = @Column(name = "hr_zone3_lower_limit")
            )
    })
    public IntensityZone heartRateZone3;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "totalTime",
                    column = @Column(name = "hr_zone4_total_time")
            ),
            @AttributeOverride(
                    name = "lowerLimit",
                    column = @Column(name = "hr_zone4_lower_limit")
            )
    })
    public IntensityZone heartRateZone4;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "totalTime",
                    column = @Column(name = "hr_zone5_total_time")
            ),
            @AttributeOverride(
                    name = "lowerLimit",
                    column = @Column(name = "hr_zone5_lower_limit")
            )
    })
    public IntensityZone heartRateZone5;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "totalTime",
                    column = @Column(name = "power_zone1_total_time")
            ),
            @AttributeOverride(
                    name = "lowerLimit",
                    column = @Column(name = "power_zone1_lower_limit")
            )
    })
    public IntensityZone powerZone1;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "totalTime",
                    column = @Column(name = "power_zone2_total_time")
            ),
            @AttributeOverride(
                    name = "lowerLimit",
                    column = @Column(name = "power_zone2_lower_limit")
            )
    })
    public IntensityZone powerZone2;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "totalTime",
                    column = @Column(name = "power_zone3_total_time")
            ),
            @AttributeOverride(
                    name = "lowerLimit",
                    column = @Column(name = "power_zone3_lower_limit")
            )
    })
    public IntensityZone powerZone3;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "totalTime",
                    column = @Column(name = "power_zone4_total_time")
            ),
            @AttributeOverride(
                    name = "lowerLimit",
                    column = @Column(name = "power_zone4_lower_limit")
            )
    })
    public IntensityZone powerZone4;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "totalTime",
                    column = @Column(name = "power_zone5_total_time")
            ),
            @AttributeOverride(
                    name = "lowerLimit",
                    column = @Column(name = "power_zone5_lower_limit")
            )
    })
    public IntensityZone powerZone5;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "totalTime",
                    column = @Column(name = "speed_zone1_total_time")
            ),
            @AttributeOverride(
                    name = "lowerLimit",
                    column = @Column(name = "speed_zone1_lower_limit")
            )
    })
    public IntensityZone speedZone1;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "totalTime",
                    column = @Column(name = "speed_zone2_total_time")
            ),
            @AttributeOverride(
                    name = "lowerLimit",
                    column = @Column(name = "speed_zone2_lower_limit")
            )
    })
    public IntensityZone speedZone2;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "totalTime",
                    column = @Column(name = "speed_zone3_total_time")
            ),
            @AttributeOverride(
                    name = "lowerLimit",
                    column = @Column(name = "speed_zone3_lower_limit")
            )
    })
    public IntensityZone speedZone3;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "totalTime",
                    column = @Column(name = "speed_zone4_total_time")
            ),
            @AttributeOverride(
                    name = "lowerLimit",
                    column = @Column(name = "speed_zone4_lower_limit")
            )
    })
    public IntensityZone speedZone4;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "totalTime",
                    column = @Column(name = "speed_zone5_total_time")
            ),
            @AttributeOverride(
                    name = "lowerLimit",
                    column = @Column(name = "speed_zone5_lower_limit")
            )
    })
    public IntensityZone speedZone5;
}