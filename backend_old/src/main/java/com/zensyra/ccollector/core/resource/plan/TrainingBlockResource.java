package com.zensyra.ccollector.core.resource.plan;

import com.zensyra.ccollector.core.dto.plan.BlockDTO;
import com.zensyra.ccollector.core.dto.plan.BlockRequest;
import com.zensyra.ccollector.core.dto.plan.BlockSummaryDTO;
import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.service.plan.TrainingBlockService;
import com.zensyra.ccollector.core.session.CurrentSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * Bloques de periodización (Fase F): estructura macro/meso/micro sobre el
 * calendario. El árbol se lee con GET; POST admite un bloque con hijos anidados
 * (una temporada de una vez) o colgar de un {@code parentId} existente.
 */
@Path("/blocks")
@Produces(MediaType.APPLICATION_JSON)
public class TrainingBlockResource {

    private final TrainingBlockService blocks;
    private final CurrentSession session;

    public TrainingBlockResource(TrainingBlockService blocks, CurrentSession session) {
        this.blocks = blocks;
        this.session = session;
    }

    @GET
    public ResponseDTO<List<BlockDTO>> list() {
        return ResponseDTO.ok(blocks.tree(session.requireUserId()));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<BlockDTO> create(BlockRequest request) {
        return ResponseDTO.ok(blocks.create(session.requireUserId(), request));
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<BlockDTO> update(@PathParam("id") Long id, BlockRequest request) {
        return ResponseDTO.ok(blocks.update(session.requireUserId(), id, request));
    }

    /** Resumen derivado: planificado vs realizado en el rango del bloque. */
    @GET
    @Path("/{id}/summary")
    public ResponseDTO<BlockSummaryDTO> summary(@PathParam("id") Long id) {
        return ResponseDTO.ok(blocks.summary(session.requireUserId(), id));
    }

    @DELETE
    @Path("/{id}")
    public ResponseDTO<Void> delete(@PathParam("id") Long id) {
        blocks.delete(session.requireUserId(), id);
        return ResponseDTO.ok(null);
    }
}
