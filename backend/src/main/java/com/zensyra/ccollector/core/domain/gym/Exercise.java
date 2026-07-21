package com.zensyra.ccollector.core.domain.gym;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Ejercicio del catálogo personal (reutilizable para construir rutinas). */
@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(nullable = false)
    public String name;

    @Column(name = "muscle_group")
    public String muscleGroup;

    public String equipment;

    @Column(length = 2000)
    public String description;
}
