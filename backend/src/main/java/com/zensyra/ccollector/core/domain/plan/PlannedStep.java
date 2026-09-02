package com.zensyra.ccollector.core.domain.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un paso estructurado de una sesión planificada. Convierte una sesión de texto
 * libre en objetivos medibles: p. ej. "4 km cal + 200 m ×5 /1'" son un paso
 * WARMUP (4000 m) y un paso INTERVAL (200 m, repeat 5, recovery 60 s).
 * Es el cimiento para planificar por día (Fase C) y comparar lo realizado con
 * lo planificado (Fase D).
 */
@Entity
@Table(name = "planned_session_steps")
public class PlannedStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "planned_session_id", nullable = false)
    public Long plannedSessionId;

    /** Orden del paso dentro de la sesión (0-based). */
    @Column(name = "order_index", nullable = false)
    public int orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public StepKind kind = StepKind.STEADY;

    /** Repeticiones del paso (5 en "200 m ×5"). */
    @Column(nullable = false)
    public int repeat = 1;

    @Column(name = "target_distance_meters")
    public Double targetDistanceMeters;

    @Column(name = "target_duration_seconds")
    public Long targetDurationSeconds;

    /** Ritmo objetivo más rápido (menos segundos por km). */
    @Column(name = "target_pace_min_sec_per_km")
    public Integer targetPaceMinSecPerKm;

    /** Ritmo objetivo más lento (más segundos por km). */
    @Column(name = "target_pace_max_sec_per_km")
    public Integer targetPaceMaxSecPerKm;

    @Column(name = "target_hr_min")
    public Integer targetHrMin;

    @Column(name = "target_hr_max")
    public Integer targetHrMax;

    /** Descanso tras cada repetición (el "/1'"). */
    @Column(name = "recovery_seconds")
    public Long recoverySeconds;

    @Column(length = 500)
    public String note;
}
