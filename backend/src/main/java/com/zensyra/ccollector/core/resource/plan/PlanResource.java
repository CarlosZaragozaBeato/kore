package com.zensyra.ccollector.core.resource.plan;

import com.zensyra.ccollector.core.dto.plan.PlanDTO;
import com.zensyra.ccollector.core.dto.plan.PlanRequest;
import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.service.plan.PlanService;
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

@Path("/plans")
@Produces(MediaType.APPLICATION_JSON)
public class PlanResource {

    private final PlanService plans;
    private final CurrentSession session;

    public PlanResource(PlanService plans, CurrentSession session) {
        this.plans = plans;
        this.session = session;
    }

    @GET
    public ResponseDTO<List<PlanDTO>> list() {
        return ResponseDTO.ok(plans.list(session.requireUserId()));
    }

    @GET
    @Path("/{id}")
    public ResponseDTO<PlanDTO> get(@PathParam("id") Long id) {
        return ResponseDTO.ok(plans.get(session.requireUserId(), id));
    }

    /** Crea un plan (o "importa" uno generado por un agente). */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<PlanDTO> create(PlanRequest request) {
        return ResponseDTO.ok(plans.create(session.requireUserId(), request));
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<PlanDTO> update(@PathParam("id") Long id, PlanRequest request) {
        return ResponseDTO.ok(plans.update(session.requireUserId(), id, request));
    }

    @DELETE
    @Path("/{id}")
    public ResponseDTO<Void> delete(@PathParam("id") Long id) {
        plans.delete(session.requireUserId(), id);
        return ResponseDTO.ok(null);
    }
}
