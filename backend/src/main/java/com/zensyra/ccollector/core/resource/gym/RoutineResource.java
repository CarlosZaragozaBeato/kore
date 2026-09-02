package com.zensyra.ccollector.core.resource.gym;

import com.zensyra.ccollector.core.dto.gym.RoutineDTO;
import com.zensyra.ccollector.core.dto.gym.RoutineRequest;
import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.service.gym.RoutineService;
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

@Path("/routines")
@Produces(MediaType.APPLICATION_JSON)
public class RoutineResource {

    private final RoutineService routines;
    private final CurrentSession session;

    public RoutineResource(RoutineService routines, CurrentSession session) {
        this.routines = routines;
        this.session = session;
    }

    @GET
    public ResponseDTO<List<RoutineDTO>> list() {
        return ResponseDTO.ok(routines.list(session.requireUserId()));
    }

    @GET
    @Path("/{id}")
    public ResponseDTO<RoutineDTO> get(@PathParam("id") Long id) {
        return ResponseDTO.ok(routines.get(session.requireUserId(), id));
    }

    /** Crea una rutina (o "importa" una generada por un agente). */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<RoutineDTO> create(RoutineRequest request) {
        return ResponseDTO.ok(routines.create(session.requireUserId(), request));
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<RoutineDTO> update(@PathParam("id") Long id, RoutineRequest request) {
        return ResponseDTO.ok(routines.update(session.requireUserId(), id, request));
    }

    @DELETE
    @Path("/{id}")
    public ResponseDTO<Void> delete(@PathParam("id") Long id) {
        routines.delete(session.requireUserId(), id);
        return ResponseDTO.ok(null);
    }
}
