package com.zensyra.ccollector.core.dto.gym;

import com.zensyra.ccollector.core.domain.gym.StrengthSession;

import java.time.LocalDate;

public record StrengthSessionDTO(
        Long id,
        LocalDate date,
        Long routineId,
        String routineName,
        String notes
) {

    public static StrengthSessionDTO from(StrengthSession s) {
        return new StrengthSessionDTO(s.id, s.date, s.routineId, s.routineName, s.notes);
    }
}
