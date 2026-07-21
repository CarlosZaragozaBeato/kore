package com.zensyra.ccollector.core.domain.gym;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Rutina de fuerza: una lista ordenada de ejercicios con series/reps/descanso. */
@Entity
@Table(name = "routines")
public class Routine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(nullable = false)
    public String name;

    @Column(length = 2000)
    public String description;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
