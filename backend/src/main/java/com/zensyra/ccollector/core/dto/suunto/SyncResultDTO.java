package com.zensyra.ccollector.core.dto.suunto;

import java.time.Instant;

/** Resumen de una sincronización con Suunto. */
public record SyncResultDTO(int imported, int skipped, int total, Instant syncedAt) {
}
