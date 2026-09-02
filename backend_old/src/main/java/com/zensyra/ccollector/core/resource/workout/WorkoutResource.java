package com.zensyra.ccollector.core.resource.workout;

import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.dto.workout.WorkoutDTO;
import com.zensyra.ccollector.core.dto.workout.WorkoutRequest;
import com.zensyra.ccollector.core.service.workout.WorkoutService;
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

@Path("/workouts")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WorkoutResource {

    private final WorkoutService workouts;
    private final CurrentSession session;

    public WorkoutResource(WorkoutService workouts, CurrentSession session) {
        this.workouts = workouts;
        this.session = session;
    }

    @GET
    public ResponseDTO<List<WorkoutDTO>> list() {
        Long userId = session.requireUserId();
        return ResponseDTO.ok(workouts.list(userId).stream().map(WorkoutDTO::from).toList());
    }

    @GET
    @Path("/{id}")
    public ResponseDTO<WorkoutDTO> get(@PathParam("id") Long id) {
        Long userId = session.requireUserId();
        return ResponseDTO.ok(WorkoutDTO.from(workouts.get(userId, id)));
    }

    @POST
    public ResponseDTO<WorkoutDTO> create(WorkoutRequest request) {
        Long userId = session.requireUserId();
        return ResponseDTO.ok(WorkoutDTO.from(workouts.create(userId, request)));
    }

    @PUT
    @Path("/{id}")
    public ResponseDTO<WorkoutDTO> update(@PathParam("id") Long id, WorkoutRequest request) {
        Long userId = session.requireUserId();
        return ResponseDTO.ok(WorkoutDTO.from(workouts.update(userId, id, request)));
    }

    @DELETE
    @Path("/{id}")
    public ResponseDTO<Void> delete(@PathParam("id") Long id) {
        Long userId = session.requireUserId();
        workouts.delete(userId, id);
        return ResponseDTO.ok(null);
    }
}
