package com.zensyra.ccollector.core.dto.kdl;

import java.util.List;

/**
 * Documento Kore Data Language (KDL): un lote de recursos de cualquier tipo y
 * versión que se puede importar/exportar entre dispositivos y versiones de la
 * app. {@code kore} identifica el formato ("kdl") y {@code kdlVersion} la
 * versión del envoltorio.
 */
public record KdlDocument(
        String kore,
        int kdlVersion,
        List<KdlItem> items
) {

    public static final String FORMAT = "kdl";
    public static final int CURRENT_VERSION = 1;
}
