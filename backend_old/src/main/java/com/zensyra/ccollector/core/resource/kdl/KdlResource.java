package com.zensyra.ccollector.core.resource.kdl;

import com.fasterxml.jackson.databind.JsonNode;
import com.zensyra.ccollector.core.dto.kdl.KdlDocument;
import com.zensyra.ccollector.core.dto.kdl.KdlImportResult;
import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.service.kdl.KdlService;
import com.zensyra.ccollector.core.session.CurrentSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Kore Data Language: import/export de recursos sueltos o en lote.
 * <ul>
 *   <li>{@code GET /kdl/export?types=workout,ingredient} — documento crudo.</li>
 *   <li>{@code POST /kdl/import} — inserta el lote en la sesión actual.</li>
 * </ul>
 */
@Path("/kdl")
@Produces(MediaType.APPLICATION_JSON)
public class KdlResource {

    private final KdlService kdl;
    private final CurrentSession session;

    public KdlResource(KdlService kdl, CurrentSession session) {
        this.kdl = kdl;
        this.session = session;
    }

    @GET
    @Path("/export")
    public KdlDocument export(@QueryParam("types") String types) {
        return kdl.export(session.require(), parseTypes(types));
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<KdlImportResult> importDoc(KdlDocument doc) {
        return ResponseDTO.ok(kdl.importDoc(session.requireUserId(), doc));
    }

    /**
     * Import por sección: el cuerpo es uno o varios recursos de {@code type}
     * (payload, array o documento KDL). Ej.: {@code POST /kdl/import/plan} con
     * el JSON de un plan completo generado por una IA.
     */
    @POST
    @Path("/import/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<KdlImportResult> importAs(@PathParam("type") String type, JsonNode body) {
        return ResponseDTO.ok(kdl.importAs(session.requireUserId(), type, body));
    }

    private static Set<String> parseTypes(String types) {
        if (types == null || types.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(types.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
