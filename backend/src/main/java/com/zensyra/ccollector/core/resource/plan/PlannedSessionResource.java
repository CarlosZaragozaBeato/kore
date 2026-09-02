package com.zensyra.ccollector.core.resource.plan;

import com.zensyra.ccollector.core.dto.plan.ComparisonDTO;
import com.zensyra.ccollector.core.dto.plan.PlanRecommendationDTO;
import com.zensyra.ccollector.core.dto.plan.PlannedSessionDTO;
import com.zensyra.ccollector.core.dto.plan.PlannedSessionRequest;
import com.zensyra.ccollector.core.dto.plan.VariantSuggestionDTO;
import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.service.plan.ComparisonService;
import com.zensyra.ccollector.core.service.plan.PlanIntelligenceService;
import com.zensyra.ccollector.core.service.plan.PlannedSessionService;
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
 * Sesiones planificadas sueltas en el calendario (Fase C): planificar por día,
 * proponer variantes y aceptar/descartar. Las sesiones de un plan se gestionan
 * en {@link PlanResource}.
 */
@Path("/planned")
@Produces(MediaType.APPLICATION_JSON)
public class PlannedSessionResource {

    private final PlannedSessionService planned;
    private final ComparisonService comparison;
    private final PlanIntelligenceService intelligence;
    private final CurrentSession session;

    public PlannedSessionResource(PlannedSessionService planned, ComparisonService comparison,
                                  PlanIntelligenceService intelligence, CurrentSession session) {
        this.planned = planned;
        this.comparison = comparison;
        this.intelligence = intelligence;
        this.session = session;
    }

    @GET
    public ResponseDTO<List<PlannedSessionDTO>> list() {
        return ResponseDTO.ok(planned.list(session.requireUserId()));
    }

    /** Recomendación de carga (descarga/mantener/subir) según las señales actuales (Fase E). */
    @GET
    @Path("/recommendation")
    public ResponseDTO<PlanRecommendationDTO> recommendation() {
        return ResponseDTO.ok(intelligence.recommend(session.requireUserId()));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<PlannedSessionDTO> create(PlannedSessionRequest request) {
        return ResponseDTO.ok(planned.create(session.requireUserId(), request));
    }

    /** Genera variantes de descarga/subida de la sesión y recomienda cuál seguir (Fase E). */
    @POST
    @Path("/{id}/variants")
    public ResponseDTO<VariantSuggestionDTO> variants(@PathParam("id") Long id) {
        return ResponseDTO.ok(intelligence.generateVariants(session.requireUserId(), id));
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<PlannedSessionDTO> update(@PathParam("id") Long id, PlannedSessionRequest request) {
        return ResponseDTO.ok(planned.update(session.requireUserId(), id, request));
    }

    /** Comparación planificado vs realizado: empareja con el entreno del día (Fase D). */
    @GET
    @Path("/{id}/comparison")
    public ResponseDTO<ComparisonDTO> comparison(@PathParam("id") Long id) {
        return ResponseDTO.ok(comparison.compare(session.requireUserId(), id));
    }

    @POST
    @Path("/{id}/accept")
    public ResponseDTO<PlannedSessionDTO> accept(@PathParam("id") Long id) {
        return ResponseDTO.ok(planned.accept(session.requireUserId(), id));
    }

    @POST
    @Path("/{id}/reject")
    public ResponseDTO<PlannedSessionDTO> reject(@PathParam("id") Long id) {
        return ResponseDTO.ok(planned.reject(session.requireUserId(), id));
    }

    @DELETE
    @Path("/{id}")
    public ResponseDTO<Void> delete(@PathParam("id") Long id) {
        planned.delete(session.requireUserId(), id);
        return ResponseDTO.ok(null);
    }
}
