package com.zensyra.domain.workout;

import com.zensyra.domain.workout.entity.WorkoutEntity;
import com.zensyra.suunto.scheduler.SuuntoSyncScheduler;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/workout")
@Produces(MediaType.APPLICATION_JSON)
public class WorkoutResource {

    @Inject
    SuuntoSyncScheduler syncScheduler;

    /**
     * Consulta y devuelve todos los entrenamientos guardados localmente en PostgreSQL.
     */
    @GET
    public List<WorkoutEntity> getAllWorkouts() {
        return WorkoutEntity.list("ORDER BY startTime DESC");
    }

    /**
     * Dispara manualmente la sincronización contra Suunto Cloud bajo demanda.
     */
    @POST
    @Path("/sync")
    public Response forceSync() {
        syncScheduler.syncWorkouts();
        return Response.ok("{\"message\": \"Sincronización completada correctamente\"}").build();
    }
}