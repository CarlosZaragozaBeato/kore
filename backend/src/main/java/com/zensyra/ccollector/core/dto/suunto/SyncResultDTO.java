package com.zensyra.ccollector.core.dto.suunto;

import java.time.Instant;

/**
 * Resumen de una sincronización con Suunto. {@code updated} son entrenos ya
 * importados que se rellenaron/corrigieron en este re-sync (backfill).
 */
public record SyncResultDTO(int imported, int updated, int skipped, int total, Instant syncedAt) {
}
