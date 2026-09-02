package com.zensyra.ccollector.core.dto.catalog;

/** Resultado de sembrar un catálogo con datos de ejemplo (idempotente por nombre). */
public record SeedResult(int added, int skipped, int total) {
}
