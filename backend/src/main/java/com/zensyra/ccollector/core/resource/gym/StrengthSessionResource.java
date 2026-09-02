package com.zensyra.ccollector.core.resource.gym;

import com.zensyra.ccollector.core.dto.gym.StrengthSessionDTO;
import com.zensyra.ccollector.core.dto.gym.StrengthSessionRequest;
import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.service.gym.StrengthSessionService;
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

@Path("/strength-sessions")
@Produces(MediaType.APPLICATION_JSON)
public class StrengthSessionResource {

    private final StrengthSessionService sessions;
    private final CurrentSession session;

    public StrengthSessionResource(StrengthSessionService sessions, CurrentSession session) {
        this.sessions = sessions;
        this.session = session;
    }

    @GET
    public ResponseDTO<List<StrengthSessionDTO>> list() {
        Long userId = session.requireUserId();
        return ResponseDTO.ok(sessions.list(userId).stream().map(StrengthSessionDTO::from).toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<StrengthSessionDTO> create(StrengthSessionRequest request) {
        return ResponseDTO.ok(StrengthSessionDTO.from(sessions.create(session.requireUserId(), request)));
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<StrengthSessionDTO> update(@PathParam("id") Long id, StrengthSessionRequest request) {
        return ResponseDTO.ok(StrengthSessionDTO.from(sessions.update(session.requireUserId(), id, request)));
    }

    @DELETE
    @Path("/{id}")
    public ResponseDTO<Void> delete(@PathParam("id") Long id) {
        sessions.delete(session.requireUserId(), id);
        return ResponseDTO.ok(null);
    }
}
