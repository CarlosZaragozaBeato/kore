package com.zensyra.ccollector.core.domain.gym;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un ejercicio dentro de una rutina. Lleva el nombre incrustado (snapshot) para
 * que la rutina sea autocontenida y portable sin depender del catálogo.
 */
@Entity
@Table(name = "routine_items")
public class RoutineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "routine_id", nullable = false)
    public Long routineId;

    @Column(nullable = false)
    public int position;

    @Column(name = "exercise_name", nullable = false)
    public String exerciseName;

    public Integer sets;

    public Integer reps;

    @Column(name = "rest_seconds")
    public Integer restSeconds;

    @Column(length = 1000)
    public String notes;
}
