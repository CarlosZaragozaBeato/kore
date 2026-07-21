package com.zensyra.ccollector.core.domain.plan;

import com.zensyra.ccollector.core.domain.workout.WorkoutType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Una sesión planificada dentro de un plan. */
@Entity
@Table(name = "planned_sessions")
public class PlannedSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "plan_id", nullable = false)
    public Long planId;

    @Column(nullable = false)
    public LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public WorkoutType type;

    @Column(name = "target_distance_meters")
    public Double targetDistanceMeters;

    @Column(name = "target_duration_seconds")
    public Long targetDurationSeconds;

    @Column(length = 2000)
    public String description;
}
