package com.zensyra.ccollector.core.domain.template;

import com.zensyra.ccollector.core.domain.workout.WorkoutType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Plantilla de sesión específica: una sesión reutilizable etiquetada por
 * disciplina, objetivo de competición y nivel (p. ej. "Series 5×1000 — media,
 * avanzado"). La estructura de la sesión va en {@link #structure} como texto.
 */
@Entity
@Table(name = "session_templates")
public class SessionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(nullable = false)
    public String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public WorkoutType discipline;

    @Enumerated(EnumType.STRING)
    public RaceGoal goal;

    @Enumerated(EnumType.STRING)
    public Level level;

    @Column(name = "target_distance_meters")
    public Double targetDistanceMeters;

    @Column(name = "target_duration_seconds")
    public Long targetDurationSeconds;

    /** Estructura de la sesión (calentamiento, series, vuelta a la calma…). */
    @Column(length = 4000)
    public String structure;

    @Column(length = 2000)
    public String notes;
}
