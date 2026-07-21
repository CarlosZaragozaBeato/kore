package com.zensyra.ccollector.core.domain.workout;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Un entrenamiento registrado. Se guarda en unidades canónicas (metros,
 * segundos, ppm) — el ritmo y otras derivadas se calculan al presentar.
 */
@Entity
@Table(name = "workouts")
public class Workout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(nullable = false)
    public LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public WorkoutType type;

    @Column(name = "distance_meters")
    public Double distanceMeters;

    @Column(name = "duration_seconds")
    public Long durationSeconds;

    @Column(name = "avg_heart_rate")
    public Integer avgHeartRate;

    @Column(name = "perceived_effort")
    public Integer perceivedEffort;

    @Column(length = 2000)
    public String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public WorkoutSource source;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
