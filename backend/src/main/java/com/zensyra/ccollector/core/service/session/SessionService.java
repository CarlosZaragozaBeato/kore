package com.zensyra.ccollector.core.service.session;

import com.zensyra.ccollector.core.domain.auth.CollectorUser;
import com.zensyra.ccollector.core.domain.gym.Exercise;
import com.zensyra.ccollector.core.domain.gym.Routine;
import com.zensyra.ccollector.core.domain.gym.RoutineItem;
import com.zensyra.ccollector.core.domain.gym.StrengthSession;
import com.zensyra.ccollector.core.domain.plan.PlannedSession;
import com.zensyra.ccollector.core.domain.plan.TrainingPlan;
import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.domain.workout.WorkoutSource;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportExercise;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportPlan;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportPlannedSession;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportRoutine;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportRoutineItem;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportStrengthSession;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportUser;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportWorkout;
import com.zensyra.ccollector.core.repository.auth.UserRepository;
import com.zensyra.ccollector.core.repository.gym.ExerciseRepository;
import com.zensyra.ccollector.core.repository.gym.RoutineItemRepository;
import com.zensyra.ccollector.core.repository.gym.RoutineRepository;
import com.zensyra.ccollector.core.repository.gym.StrengthSessionRepository;
import com.zensyra.ccollector.core.repository.plan.PlanRepository;
import com.zensyra.ccollector.core.repository.plan.PlannedSessionRepository;
import com.zensyra.ccollector.core.repository.workout.WorkoutRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import java.time.Instant;

@ApplicationScoped
public class SessionService {

    private final UserRepository users;
    private final WorkoutRepository workouts;
    private final PlanRepository plans;
    private final PlannedSessionRepository plannedSessions;
    private final ExerciseRepository exercises;
    private final RoutineRepository routines;
    private final RoutineItemRepository routineItems;
    private final StrengthSessionRepository strengthSessions;

    public SessionService(UserRepository users, WorkoutRepository workouts,
                          PlanRepository plans, PlannedSessionRepository plannedSessions,
                          ExerciseRepository exercises, RoutineRepository routines,
                          RoutineItemRepository routineItems, StrengthSessionRepository strengthSessions) {
        this.users = users;
        this.workouts = workouts;
        this.plans = plans;
        this.plannedSessions = plannedSessions;
        this.exercises = exercises;
        this.routines = routines;
        this.routineItems = routineItems;
        this.strengthSessions = strengthSessions;
    }

    /** Vuelca la sesión completa del usuario a un documento portable. */
    public SessionExportDTO export(CollectorUser user) {
        var exportWorkouts = workouts.listByUser(user.id).stream()
                .map(w -> new ExportWorkout(
                        w.date, w.type, w.distanceMeters, w.durationSeconds,
                        w.avgHeartRate, w.perceivedEffort, w.notes, w.source, w.createdAt))
                .toList();
        var exportPlans = plans.listByUser(user.id).stream()
                .map(p -> new ExportPlan(
                        p.name, p.goal, p.startDate, p.endDate, p.createdAt,
                        plannedSessions.listByPlan(p.id).stream()
                                .map(s -> new ExportPlannedSession(
                                        s.date, s.type, s.targetDistanceMeters,
                                        s.targetDurationSeconds, s.description))
                                .toList()))
                .toList();
        var exportExercises = exercises.listByUser(user.id).stream()
                .map(e -> new ExportExercise(e.name, e.muscleGroup, e.equipment, e.description))
                .toList();
        var exportRoutines = routines.listByUser(user.id).stream()
                .map(r -> new ExportRoutine(
                        r.name, r.description, r.createdAt,
                        routineItems.listByRoutine(r.id).stream()
                                .map(i -> new ExportRoutineItem(
                                        i.exerciseName, i.sets, i.reps, i.restSeconds, i.notes))
                                .toList()))
                .toList();
        var exportStrength = strengthSessions.listByUser(user.id).stream()
                .map(s -> new ExportStrengthSession(s.date, s.routineName, s.notes, s.createdAt))
                .toList();

        return new SessionExportDTO(
                SessionExportDTO.CURRENT_SCHEMA_VERSION,
                Instant.now(),
                new ExportUser(user.username, user.createdAt),
                exportWorkouts,
                exportPlans,
                exportExercises,
                exportRoutines,
                exportStrength);
    }

    /**
     * Reconstruye una sesión desde un documento. Pensado para un dispositivo
     * nuevo: si el username ya existe, rechaza (409) para no pisar datos.
     */
    @Transactional
    public CollectorUser importSession(SessionExportDTO doc) {
        validate(doc);

        String username = doc.user().username().trim();
        if (users.findByUsername(username).isPresent()) {
            throw new ClientErrorException(
                    "Ya existe una sesión '" + username + "'; elimínala antes de importar",
                    Response.Status.CONFLICT);
        }

        CollectorUser user = new CollectorUser();
        user.username = username;
        user.createdAt = doc.user().createdAt() != null ? doc.user().createdAt() : Instant.now();
        users.persist(user);

        if (doc.workouts() != null) {
            for (ExportWorkout ew : doc.workouts()) {
                Workout w = new Workout();
                w.userId = user.id;
                w.date = ew.date();
                w.type = ew.type();
                w.distanceMeters = ew.distanceMeters();
                w.durationSeconds = ew.durationSeconds();
                w.avgHeartRate = ew.avgHeartRate();
                w.perceivedEffort = ew.perceivedEffort();
                w.notes = ew.notes();
                w.source = ew.source() != null ? ew.source() : WorkoutSource.MANUAL;
                w.createdAt = ew.createdAt() != null ? ew.createdAt() : Instant.now();
                workouts.persist(w);
            }
        }

        if (doc.plans() != null) {
            for (ExportPlan ep : doc.plans()) {
                TrainingPlan plan = new TrainingPlan();
                plan.userId = user.id;
                plan.name = ep.name();
                plan.goal = ep.goal();
                plan.startDate = ep.startDate();
                plan.endDate = ep.endDate();
                plan.createdAt = ep.createdAt() != null ? ep.createdAt() : Instant.now();
                plans.persist(plan);
                if (ep.sessions() != null) {
                    for (ExportPlannedSession es : ep.sessions()) {
                        PlannedSession s = new PlannedSession();
                        s.planId = plan.id;
                        s.date = es.date();
                        s.type = es.type();
                        s.targetDistanceMeters = es.targetDistanceMeters();
                        s.targetDurationSeconds = es.targetDurationSeconds();
                        s.description = es.description();
                        plannedSessions.persist(s);
                    }
                }
            }
        }

        if (doc.exercises() != null) {
            for (ExportExercise ee : doc.exercises()) {
                Exercise e = new Exercise();
                e.userId = user.id;
                e.name = ee.name();
                e.muscleGroup = ee.muscleGroup();
                e.equipment = ee.equipment();
                e.description = ee.description();
                exercises.persist(e);
            }
        }

        if (doc.routines() != null) {
            for (ExportRoutine er : doc.routines()) {
                Routine routine = new Routine();
                routine.userId = user.id;
                routine.name = er.name();
                routine.description = er.description();
                routine.createdAt = er.createdAt() != null ? er.createdAt() : Instant.now();
                routines.persist(routine);
                if (er.items() != null) {
                    int position = 0;
                    for (ExportRoutineItem ei : er.items()) {
                        RoutineItem item = new RoutineItem();
                        item.routineId = routine.id;
                        item.position = position++;
                        item.exerciseName = ei.exerciseName();
                        item.sets = ei.sets();
                        item.reps = ei.reps();
                        item.restSeconds = ei.restSeconds();
                        item.notes = ei.notes();
                        routineItems.persist(item);
                    }
                }
            }
        }

        if (doc.strengthSessions() != null) {
            for (ExportStrengthSession es : doc.strengthSessions()) {
                StrengthSession s = new StrengthSession();
                s.userId = user.id;
                s.date = es.date();
                // el id de rutina no es portable; se conserva solo el nombre (snapshot).
                s.routineName = es.routineName();
                s.notes = es.notes();
                s.createdAt = es.createdAt() != null ? es.createdAt() : Instant.now();
                strengthSessions.persist(s);
            }
        }
        return user;
    }

    private void validate(SessionExportDTO doc) {
        if (doc == null || doc.user() == null
                || doc.user().username() == null || doc.user().username().isBlank()) {
            throw new BadRequestException("Documento de sesión inválido: falta el usuario");
        }
        if (doc.schemaVersion() > SessionExportDTO.CURRENT_SCHEMA_VERSION) {
            throw new BadRequestException("schemaVersion " + doc.schemaVersion()
                    + " no soportado (máximo " + SessionExportDTO.CURRENT_SCHEMA_VERSION + ")");
        }
    }
}
