package com.zensyra.ccollector.core.dto.kdl;

import java.util.List;
import java.util.Map;

/**
 * Resultado de importar un documento KDL. Cada item se procesa de forma
 * aislada: uno inválido no aborta el lote (se cuenta en {@code skipped} y su
 * motivo en {@code errors}).
 */
public record KdlImportResult(
        int imported,
        int skipped,
        List<String> errors,
        Map<String, Integer> byType
) {
}
