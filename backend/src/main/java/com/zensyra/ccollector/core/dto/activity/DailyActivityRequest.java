package com.zensyra.ccollector.core.dto.activity;

import java.time.LocalDate;

/** Alta/actualización de la actividad de un día (upsert por fecha). */
public record DailyActivityRequest(LocalDate date, Integer steps, Double burnedKcal) {
}
