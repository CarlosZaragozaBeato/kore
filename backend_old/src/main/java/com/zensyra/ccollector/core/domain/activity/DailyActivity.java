package com.zensyra.ccollector.core.domain.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Actividad diaria del usuario (pasos y quema total del día). Un registro por
 * fecha (upsert). La {@code burnedKcal} es la quema TOTAL del día (basal +
 * actividad + entrenos), tal y como la reporta Suunto; por eso, cuando existe,
 * sustituye a la estimación por entrenos en el balance energético (evita
 * contar dos veces).
 *
 * De momento se rellena a mano o por import; el fetch automático de la
 * actividad 24/7 de Suunto (endpoint aparte del resumen de workouts) queda
 * pendiente.
 */
@Entity
@Table(name = "daily_activity")
public class DailyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(nullable = false)
    public LocalDate date;

    @Column(name = "steps")
    public Integer steps;

    @Column(name = "burned_kcal")
    public Double burnedKcal;
}
