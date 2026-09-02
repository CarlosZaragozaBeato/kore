package com.zensyra.ccollector.core.resource.nutrition;

import com.zensyra.ccollector.core.dto.nutrition.DietPlanDTO;
import com.zensyra.ccollector.core.dto.nutrition.DietPlanRequest;
import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.service.nutrition.DietPlanService;
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

@Path("/diet-plans")
@Produces(MediaType.APPLICATION_JSON)
public class DietPlanResource {

    private final DietPlanService plans;
    private final CurrentSession session;

    public DietPlanResource(DietPlanService plans, CurrentSession session) {
        this.plans = plans;
        this.session = session;
    }

    @GET
    public ResponseDTO<List<DietPlanDTO>> list() {
        return ResponseDTO.ok(plans.list(session.requireUserId()));
    }

    @GET
    @Path("/{id}")
    public ResponseDTO<DietPlanDTO> get(@PathParam("id") Long id) {
        return ResponseDTO.ok(plans.get(session.requireUserId(), id));
    }

    /** Crea un plan de dieta (o "importa" uno generado por un agente). */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<DietPlanDTO> create(DietPlanRequest request) {
        return ResponseDTO.ok(plans.create(session.requireUserId(), request));
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<DietPlanDTO> update(@PathParam("id") Long id, DietPlanRequest request) {
        return ResponseDTO.ok(plans.update(session.requireUserId(), id, request));
    }

    @DELETE
    @Path("/{id}")
    public ResponseDTO<Void> delete(@PathParam("id") Long id) {
        plans.delete(session.requireUserId(), id);
        return ResponseDTO.ok(null);
    }
}
