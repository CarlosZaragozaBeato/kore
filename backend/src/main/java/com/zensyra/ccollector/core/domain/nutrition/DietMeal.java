package com.zensyra.ccollector.core.domain.nutrition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Una comida planificada dentro de un plan de dieta (snapshot del nombre de receta). */
@Entity
@Table(name = "diet_meals")
public class DietMeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "diet_plan_id", nullable = false)
    public Long dietPlanId;

    @Column(nullable = false)
    public LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    public MealType mealType;

    @Column(name = "recipe_name")
    public String recipeName;

    @Column(length = 1000)
    public String notes;
}
