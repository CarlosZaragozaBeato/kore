package com.zensyra.ccollector.core.resource.weight;

import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.dto.weight.WeightEntryDTO;
import com.zensyra.ccollector.core.dto.weight.WeightEntryRequest;
import com.zensyra.ccollector.core.dto.weight.WeightGoalDTO;
import com.zensyra.ccollector.core.dto.weight.WeightGoalRequest;
import com.zensyra.ccollector.core.service.weight.WeightService;
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

@Path("/weight")
@Produces(MediaType.APPLICATION_JSON)
public class WeightResource {

    private final WeightService weight;
    private final CurrentSession session;

    public WeightResource(WeightService weight, CurrentSession session) {
        this.weight = weight;
        this.session = session;
    }

    @GET
    public ResponseDTO<List<WeightEntryDTO>> list() {
        Long userId = session.requireUserId();
        return ResponseDTO.ok(weight.list(userId).stream().map(WeightEntryDTO::from).toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<WeightEntryDTO> upsert(WeightEntryRequest request) {
        return ResponseDTO.ok(WeightEntryDTO.from(weight.upsert(session.requireUserId(), request)));
    }

    @DELETE
    @Path("/{id}")
    public ResponseDTO<Void> delete(@PathParam("id") Long id) {
        weight.delete(session.requireUserId(), id);
        return ResponseDTO.ok(null);
    }

    @GET
    @Path("/goal")
    public ResponseDTO<WeightGoalDTO> goal() {
        return ResponseDTO.ok(WeightGoalDTO.from(weight.goal(session.requireUserId())));
    }

    @PUT
    @Path("/goal")
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<WeightGoalDTO> setGoal(WeightGoalRequest request) {
        return ResponseDTO.ok(WeightGoalDTO.from(weight.setGoal(session.requireUserId(), request)));
    }
}
