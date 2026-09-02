package com.zensyra.ccollector.core.dto.agent;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Mapa estable y descriptivo de la API para agentes: qué recursos existen y
 * cómo leerlos/escribirlos. Pensado para que un LLM lo consuma de una vez y
 * sepa navegar el resto de endpoints.
 */
public record AgentManifestDTO(
        int schemaVersion,
        String apiBase,
        String sessionHeader,
        String description,
        List<ResourceDescriptor> resources,
        Io io,
        Docs docs
) {

    public record ResourceDescriptor(
            String name,
            String description,
            String list,
            String get,
            String create,
            String update,
            String delete
    ) {
    }

    public record Io(
            String export,
            @JsonProperty("import") String importEndpoint
    ) {
    }

    public record Docs(String openapi, String swaggerUi, String context) {
    }
}
