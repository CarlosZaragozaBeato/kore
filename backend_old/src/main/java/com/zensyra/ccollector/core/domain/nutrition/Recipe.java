package com.zensyra.ccollector.core.domain.nutrition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Receta. Macros como totales de la receta (con {@code servings} para derivar
 * el valor por ración). Los pasos van como texto multilínea.
 */
@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(nullable = false)
    public String name;

    @Column(length = 2000)
    public String description;

    public Integer servings;

    public Double calories;

    public Double protein;

    public Double carbs;

    public Double fat;

    @Column(length = 4000)
    public String steps;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
