package com.zensyra.ccollector.core.dto.activity;

import com.zensyra.ccollector.core.domain.activity.DailyActivity;

import java.time.LocalDate;

public record DailyActivityDTO(Long id, LocalDate date, Integer steps, Double burnedKcal) {

    public static DailyActivityDTO from(DailyActivity a) {
        return new DailyActivityDTO(a.id, a.date, a.steps, a.burnedKcal);
    }
}
