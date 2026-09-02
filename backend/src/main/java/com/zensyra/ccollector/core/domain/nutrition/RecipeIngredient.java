package com.zensyra.ccollector.core.domain.nutrition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Un ingrediente de una receta. */
@Entity
@Table(name = "recipe_ingredients")
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "recipe_id", nullable = false)
    public Long recipeId;

    @Column(nullable = false)
    public int position;

    @Column(nullable = false)
    public String name;

    public Double quantity;

    public String unit;

    /** Enlace opcional al ingrediente del catálogo (para derivar macros). */
    @Column(name = "ingredient_id")
    public Long ingredientId;
}
