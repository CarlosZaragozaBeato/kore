package com.zensyra.ccollector.core.domain.nutrition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/** Plan de dieta por periodo, con objetivos nutricionales y comidas por día. */
@Entity
@Table(name = "diet_plans")
public class DietPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(nullable = false)
    public String name;

    @Column(name = "start_date", nullable = false)
    public LocalDate startDate;

    @Column(name = "end_date")
    public LocalDate endDate;

    @Column(name = "target_calories")
    public Double targetCalories;

    @Column(name = "target_protein")
    public Double targetProtein;

    @Column(name = "target_carbs")
    public Double targetCarbs;

    @Column(name = "target_fat")
    public Double targetFat;

    @Column(length = 2000)
    public String notes;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
