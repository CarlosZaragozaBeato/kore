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

    @Column(name = "max_heart_rate")
    public Integer maxHeartRate;

    /** Energía gastada en kcal (de Suunto; opcional en entrenos manuales). */
    @Column(name = "energy_kcal")
    public Double energyKcal;

    /**
     * Pasos totales del entreno (de Suunto). Con la distancia y el tiempo se
     * derivan cadencia (pasos/min) y longitud de zancada (m/paso) al presentar.
     */
    @Column(name = "step_count")
    public Integer stepCount;

    @Column(name = "perceived_effort")
    public Integer perceivedEffort;

    @Column(length = 2000)
    public String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public WorkoutSource source;

    /** Id del entreno en el origen (Suunto), para deduplicar. Null si es manual. */
    @Column(name = "source_id")
    public String sourceId;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
