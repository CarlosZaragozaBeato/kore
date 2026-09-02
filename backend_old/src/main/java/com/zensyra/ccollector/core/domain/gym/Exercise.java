package com.zensyra.ccollector.core.domain.gym;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    /** Fase/rol: calentamiento, fuerza (principal) o recuperación. */
    @Enumerated(EnumType.STRING)
    public ExerciseCategory category;

    /** true si necesita material; el material concreto va en {@link #equipment}. */
    @Column(name = "requires_equipment")
    public Boolean requiresEquipment;

    public String equipment;

    /** Para qué y por qué sirve el ejercicio. */
    @Column(length = 2000)
    public String description;

    /** Imagen o animación (gif) que muestra cómo se ejecuta. URL o ruta. */
    @Column(name = "image_url", length = 500)
    public String imageUrl;

    /** Información de realización: técnica paso a paso. */
    @Column(length = 2000)
    public String instructions;

    /**
     * Intensidad metabólica (MET) para estimar el gasto energético. kcal/min ≈
     * MET · 3.5 · pesoKg / 200. Se deja al lector calcular con su peso.
     */
    @Column(name = "met_value")
    public Double metValue;
}
