package com.zensyra.ccollector.core.dto.weight;

import java.time.LocalDate;

/** Alta/actualización de una medición de peso (upsert por fecha). */
public record WeightEntryRequest(LocalDate date, Double weightKg) {
}
