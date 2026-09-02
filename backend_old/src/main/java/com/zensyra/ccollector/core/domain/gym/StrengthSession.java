package com.zensyra.ccollector.core.domain.gym;

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
 * Sesión de fuerza en el calendario. Puede estar planificada para un día
 * (estado PLANNED) o ya realizada (estado DONE). Puede referenciar una rutina
 * (guarda también su nombre como snapshot) o ser libre.
 */
@Entity
@Table(name = "strength_sessions")
public class StrengthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(nullable = false)
    public LocalDate date;

    @Column(name = "routine_id")
    public Long routineId;

    @Column(name = "routine_name")
    public String routineName;

    @Column(length = 2000)
    public String notes;

    /** Planificada (por hacer) o realizada. Por defecto DONE (registro). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public StrengthStatus status = StrengthStatus.DONE;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
