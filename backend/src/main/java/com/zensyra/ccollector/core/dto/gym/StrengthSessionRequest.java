package com.zensyra.ccollector.core.dto.gym;

import com.zensyra.ccollector.core.domain.gym.StrengthStatus;

import java.time.LocalDate;

public record StrengthSessionRequest(
        LocalDate date, Long routineId, String notes, StrengthStatus status) {
}
