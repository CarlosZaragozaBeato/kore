package com.zensyra.ccollector.core.dto.weight;

import com.zensyra.ccollector.core.domain.weight.WeightEntry;

import java.time.LocalDate;

public record WeightEntryDTO(Long id, LocalDate date, Double weightKg) {

    public static WeightEntryDTO from(WeightEntry e) {
        return new WeightEntryDTO(e.id, e.date, e.weightKg);
    }
}
