package com.zensyra.ccollector.core.domain.nutrition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Ingrediente del catálogo personal. Los valores nutricionales se guardan
 * SIEMPRE por 100 (g o ml, según {@link #baseUnit}) para poder escalar a
 * cualquier cantidad al construir recetas y dietas.
 */
@Entity
@Table(name = "ingredients")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(nullable = false)
    public String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_unit", nullable = false)
    public BaseUnit baseUnit;

    /** Imagen del ingrediente para el catálogo visual. URL o ruta. Opcional. */
    @Column(name = "image_url", length = 500)
    public String imageUrl;

    /** kcal por 100 g/ml. */
    public Double calories;

    /** Proteína (g) por 100 g/ml. */
    public Double protein;

    /** Carbohidratos (g) por 100 g/ml. */
    public Double carbs;

    /** Grasa (g) por 100 g/ml. */
    public Double fat;

    /** Fibra (g) por 100 g/ml. Opcional. */
    public Double fiber;

    /** Azúcares (g) por 100 g/ml. Opcional. */
    public Double sugars;

    /** Sodio (mg) por 100 g/ml. Opcional. */
    public Double sodium;
}
