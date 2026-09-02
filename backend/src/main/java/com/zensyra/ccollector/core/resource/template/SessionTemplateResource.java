package com.zensyra.ccollector.core.resource.template;

import com.zensyra.ccollector.core.dto.catalog.SeedResult;
import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.dto.template.SessionTemplateDTO;
import com.zensyra.ccollector.core.dto.template.SessionTemplateRequest;
import com.zensyra.ccollector.core.service.template.SessionTemplateService;
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

@Path("/session-templates")
@Produces(MediaType.APPLICATION_JSON)
public class SessionTemplateResource {

    private final SessionTemplateService templates;
    private final CurrentSession session;

    public SessionTemplateResource(SessionTemplateService templates, CurrentSession session) {
        this.templates = templates;
        this.session = session;
    }

    @GET
    public ResponseDTO<List<SessionTemplateDTO>> list() {
        Long userId = session.requireUserId();
        return ResponseDTO.ok(templates.list(userId).stream().map(SessionTemplateDTO::from).toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<SessionTemplateDTO> create(SessionTemplateRequest request) {
        return ResponseDTO.ok(SessionTemplateDTO.from(templates.create(session.requireUserId(), request)));
    }

    /** Siembra el catálogo con plantillas de ejemplo por disciplina/objetivo/nivel. */
    @POST
    @Path("/seed")
    public ResponseDTO<SeedResult> seed() {
        return ResponseDTO.ok(templates.seed(session.requireUserId()));
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<SessionTemplateDTO> update(@PathParam("id") Long id, SessionTemplateRequest request) {
        return ResponseDTO.ok(SessionTemplateDTO.from(templates.update(session.requireUserId(), id, request)));
    }

    @DELETE
    @Path("/{id}")
    public ResponseDTO<Void> delete(@PathParam("id") Long id) {
        templates.delete(session.requireUserId(), id);
        return ResponseDTO.ok(null);
    }
}
