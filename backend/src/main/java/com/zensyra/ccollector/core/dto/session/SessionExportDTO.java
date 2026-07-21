package com.zensyra.ccollector.core.dto.session;

import com.zensyra.ccollector.core.domain.workout.WorkoutSource;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Documento portable de una sesión completa. Es el formato de intercambio para
 * llevar tus datos entre dispositivos y para que agentes los lean/escriban.
 * No incluye ids de base de datos: se reasignan al importar.
 *
 * v1: usuario + workouts. v2: añade planes de entrenamiento (retrocompatible;
 * un documento v1 se importa sin planes).
 */
public record SessionExportDTO(
        int schemaVersion,
        Instant exportedAt,
        ExportUser user,
        List<ExportWorkout> workouts,
        List<ExportPlan> plans
) {

    /** Versión actual del formato. Súbela al cambiar la estructura. */
    public static final int CURRENT_SCHEMA_VERSION = 2;

    public record ExportUser(String username, Instant createdAt) {
    }

    public record ExportWorkout(
            LocalDate date,
            WorkoutType type,
            Double distanceMeters,
            Long durationSeconds,
            Integer avgHeartRate,
            Integer perceivedEffort,
            String notes,
            WorkoutSource source,
            Instant createdAt
    ) {
    }

    public record ExportPlan(
            String name,
            String goal,
            LocalDate startDate,
            LocalDate endDate,
            Instant createdAt,
            List<ExportPlannedSession> sessions
    ) {
    }

    public record ExportPlannedSession(
            LocalDate date,
            WorkoutType type,
            Double targetDistanceMeters,
            Long targetDurationSeconds,
            String description
    ) {
    }
}
