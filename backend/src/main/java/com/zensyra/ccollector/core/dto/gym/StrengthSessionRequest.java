package com.zensyra.ccollector.core.dto.gym;

import java.time.LocalDate;

public record StrengthSessionRequest(LocalDate date, Long routineId, String notes) {
}
