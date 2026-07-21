package com.zensyra.ccollector.core.service.session;

import com.zensyra.ccollector.core.domain.auth.CollectorUser;
import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.domain.workout.WorkoutSource;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportUser;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportWorkout;
import com.zensyra.ccollector.core.repository.auth.UserRepository;
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

    public SessionService(UserRepository users, WorkoutRepository workouts) {
        this.users = users;
        this.workouts = workouts;
    }

    /** Vuelca la sesión completa del usuario a un documento portable. */
    public SessionExportDTO export(CollectorUser user) {
        var exportWorkouts = workouts.listByUser(user.id).stream()
                .map(w -> new ExportWorkout(
                        w.date, w.type, w.distanceMeters, w.durationSeconds,
                        w.avgHeartRate, w.perceivedEffort, w.notes, w.source, w.createdAt))
                .toList();
        return new SessionExportDTO(
                SessionExportDTO.CURRENT_SCHEMA_VERSION,
                Instant.now(),
                new ExportUser(user.username, user.createdAt),
                exportWorkouts);
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
