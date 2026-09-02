package com.zensyra.ccollector.core.dto.suunto;

import java.time.LocalDate;

/**
 * Resultado de aplicar la retención mínima de Suunto: cuántos entrenos se
 * borraron y la fecha de corte (se conserva todo a partir de ella).
 */
public record PurgeResultDTO(long purged, LocalDate cutoff) {
}
