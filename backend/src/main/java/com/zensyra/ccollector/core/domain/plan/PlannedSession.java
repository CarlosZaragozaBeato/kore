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

/**
 * Una sesión planificada. Puede pertenecer a un plan ({@code planId}) o vivir
 * suelta en el calendario ({@code planId} nulo), con estado y agrupación por
 * variantes para elegir entre propuestas de un mismo día.
 */
@Entity
@Table(name = "planned_sessions")
public class PlannedSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** Plan al que pertenece, o nulo si es una planificación suelta de calendario. */
    @Column(name = "plan_id")
    public Long planId;

    /** Dueño. Siempre presente; en las de plan se hereda del plan. */
    @Column(name = "user_id", nullable = false)
    public Long userId;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public SessionStatus status = SessionStatus.ACCEPTED;

    /** Agrupa variantes de un mismo día (todas comparten grupo); nulo si no es variante. */
    @Column(name = "variant_group", length = 40)
    public String variantGroup;

    /** Etiqueta de la variante (p. ej. "Carga normal", "Descarga"). */
    @Column(name = "variant_label", length = 120)
    public String variantLabel;
}
