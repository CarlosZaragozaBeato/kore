package com.zensyra.ccollector.core.resource.activity;

import com.zensyra.ccollector.core.dto.activity.DailyActivityDTO;
import com.zensyra.ccollector.core.dto.activity.DailyActivityRequest;
import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.service.activity.DailyActivityService;
import com.zensyra.ccollector.core.session.CurrentSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/activity/daily")
@Produces(MediaType.APPLICATION_JSON)
public class DailyActivityResource {

    private final DailyActivityService activity;
    private final CurrentSession session;

    public DailyActivityResource(DailyActivityService activity, CurrentSession session) {
        this.activity = activity;
        this.session = session;
    }

    @GET
    public ResponseDTO<List<DailyActivityDTO>> list() {
        Long userId = session.requireUserId();
        return ResponseDTO.ok(activity.list(userId).stream().map(DailyActivityDTO::from).toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<DailyActivityDTO> upsert(DailyActivityRequest request) {
        return ResponseDTO.ok(DailyActivityDTO.from(activity.upsert(session.requireUserId(), request)));
    }

    @DELETE
    @Path("/{id}")
    public ResponseDTO<Void> delete(@PathParam("id") Long id) {
        activity.delete(session.requireUserId(), id);
        return ResponseDTO.ok(null);
    }
}
