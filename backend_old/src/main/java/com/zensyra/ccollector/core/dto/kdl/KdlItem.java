package com.zensyra.ccollector.core.dto.kdl;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Un recurso en Kore Data Language: envoltorio común con el tipo, su versión de
 * esquema y el contenido ({@code payload}, sin ids de BD). El payload es
 * flexible ({@link JsonNode}) para tolerar campos desconocidos entre versiones.
 */
public record KdlItem(
        String koreType,
        int schemaVersion,
        JsonNode payload
) {
}
