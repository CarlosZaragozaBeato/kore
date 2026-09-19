package com.zensyra.domain.workout;

import com.zensyra.domain.workout.entity.WorkoutEntity;
import com.zensyra.suunto.scheduler.SuuntoSyncScheduler;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Path("/workout")
@Produces(MediaType.APPLICATION_JSON)
public class WorkoutResource {

    @Inject
    SuuntoSyncScheduler syncScheduler;

    @GET
    public List<WorkoutEntity> getAllWorkouts(
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to
    ) {
        return findWorkouts(from, to);
    }

    @GET
    @Path("/summary")
    public WorkoutSummary getSummary(
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to
    ) {
        List<WorkoutEntity> workouts = findWorkouts(from, to);

        double distanceMeters = workouts.stream()
                .filter(workout -> workout.distanceMeters != null)
                .mapToDouble(workout -> workout.distanceMeters)
                .sum();

        double durationSeconds = workouts.stream()
                .filter(workout -> workout.durationSeconds != null)
                .mapToDouble(workout -> workout.durationSeconds)
                .sum();

        double tssHrTotal = workouts.stream()
                .filter(workout -> workout.trainingLoad != null
                        && workout.trainingLoad.hr != null
                        && workout.trainingLoad.hr.trainingStressScore != null)
                .mapToDouble(workout -> workout.trainingLoad.hr.trainingStressScore)
                .sum();

        int tssHrAvailableSessions = (int) workouts.stream()
                .filter(workout -> workout.trainingLoad != null
                        && workout.trainingLoad.hr != null
                        && workout.trainingLoad.hr.trainingStressScore != null)
                .count();

        double tssPowerTotal = workouts.stream()
                .filter(workout -> workout.trainingLoad != null
                        && workout.trainingLoad.power != null
                        && workout.trainingLoad.power.trainingStressScore != null)
                .mapToDouble(workout -> workout.trainingLoad.power.trainingStressScore)
                .sum();

        int tssPowerAvailableSessions = (int) workouts.stream()
                .filter(workout -> workout.trainingLoad != null
                        && workout.trainingLoad.power != null
                        && workout.trainingLoad.power.trainingStressScore != null)
                .count();

        double tssPaceTotal = workouts.stream()
                .filter(workout -> workout.trainingLoad != null
                        && workout.trainingLoad.pace != null
                        && workout.trainingLoad.pace.trainingStressScore != null)
                .mapToDouble(workout -> workout.trainingLoad.pace.trainingStressScore)
                .sum();

        int tssPaceAvailableSessions = (int) workouts.stream()
                .filter(workout -> workout.trainingLoad != null
                        && workout.trainingLoad.pace != null
                        && workout.trainingLoad.pace.trainingStressScore != null)
                .count();

        double tssMetTotal = workouts.stream()
                .filter(workout -> workout.trainingLoad != null
                        && workout.trainingLoad.met != null
                        && workout.trainingLoad.met.trainingStressScore != null)
                .mapToDouble(workout -> workout.trainingLoad.met.trainingStressScore)
                .sum();

        int tssMetAvailableSessions = (int) workouts.stream()
                .filter(workout -> workout.trainingLoad != null
                        && workout.trainingLoad.met != null
                        && workout.trainingLoad.met.trainingStressScore != null)
                .count();

        return new WorkoutSummary(
                workouts.size(),
                distanceMeters,
                durationSeconds,
                tssHrTotal,
                tssHrAvailableSessions,
                tssPowerTotal,
                tssPowerAvailableSessions,
                tssPaceTotal,
                tssPaceAvailableSessions,
                tssMetTotal,
                tssMetAvailableSessions
        );
    }

    @GET
    @Path("/{workoutKey}")
    public Response getWorkout(@PathParam("workoutKey") String workoutKey) {
        WorkoutEntity workout = WorkoutEntity.findById(workoutKey);

        if (workout == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(workout).build();
    }

    @POST
    @Path("/sync")
    public Response forceSync() {
        syncScheduler.syncWorkouts();

        return Response.ok(
                "{\"message\": \"Sincronización completada correctamente\"}"
        ).build();
    }

    private List<WorkoutEntity> findWorkouts(
            LocalDate from,
            LocalDate to
    ) {
        if (from == null && to == null) {
            return WorkoutEntity.list("ORDER BY startTime DESC");
        }

        OffsetDateTime fromDateTime = from != null
                ? from.atStartOfDay().atOffset(ZoneOffset.UTC)
                : null;

        OffsetDateTime toDateTime = to != null
                ? to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
                : null;

        if (fromDateTime != null && toDateTime != null) {
            return WorkoutEntity.list(
                    "startTime >= ?1 AND startTime < ?2 ORDER BY startTime DESC",
                    fromDateTime,
                    toDateTime
            );
        }

        if (fromDateTime != null) {
            return WorkoutEntity.list(
                    "startTime >= ?1 ORDER BY startTime DESC",
                    fromDateTime
            );
        }

        return WorkoutEntity.list(
                "startTime < ?1 ORDER BY startTime DESC",
                toDateTime
        );
    }

    public record WorkoutSummary(
            int sessions,
            double distanceMeters,
            double durationSeconds,
            double tssHrTotal,
            int tssHrAvailableSessions,
            double tssPowerTotal,
            int tssPowerAvailableSessions,
            double tssPaceTotal,
            int tssPaceAvailableSessions,
            double tssMetTotal,
            int tssMetAvailableSessions
    ) {
    }
}