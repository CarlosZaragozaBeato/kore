package com.zensyra.ccollector.core.resource.gym;

import com.zensyra.ccollector.core.dto.catalog.SeedResult;
import com.zensyra.ccollector.core.dto.gym.ExerciseDTO;
import com.zensyra.ccollector.core.dto.gym.ExerciseRequest;
import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.service.gym.ExerciseService;
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

@Path("/exercises")
@Produces(MediaType.APPLICATION_JSON)
public class ExerciseResource {

    private final ExerciseService exercises;
    private final CurrentSession session;

    public ExerciseResource(ExerciseService exercises, CurrentSession session) {
        this.exercises = exercises;
        this.session = session;
    }

    @GET
    public ResponseDTO<List<ExerciseDTO>> list() {
        Long userId = session.requireUserId();
        return ResponseDTO.ok(exercises.list(userId).stream().map(ExerciseDTO::from).toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<ExerciseDTO> create(ExerciseRequest request) {
        return ResponseDTO.ok(ExerciseDTO.from(exercises.create(session.requireUserId(), request)));
    }

    /** Siembra el catálogo con ejercicios de ejemplo (calentamiento/fuerza/recuperación). */
    @POST
    @Path("/seed")
    public ResponseDTO<SeedResult> seed() {
        return ResponseDTO.ok(exercises.seed(session.requireUserId()));
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<ExerciseDTO> update(@PathParam("id") Long id, ExerciseRequest request) {
        return ResponseDTO.ok(ExerciseDTO.from(exercises.update(session.requireUserId(), id, request)));
    }

    @DELETE
    @Path("/{id}")
    public ResponseDTO<Void> delete(@PathParam("id") Long id) {
        exercises.delete(session.requireUserId(), id);
        return ResponseDTO.ok(null);
    }
}
