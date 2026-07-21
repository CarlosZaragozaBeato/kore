package com.zensyra.ccollector.core.dto.session;

import com.zensyra.ccollector.core.domain.nutrition.MealType;
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
 * v1: usuario + workouts. v2: añade planes. v3: añade gimnasio (ejercicios,
 * rutinas, sesiones de fuerza). v4: añade nutrición (recetas, dietas).
 * Retrocompatible: un documento de versión menor se importa sin las secciones
 * que no incluya.
 */
public record SessionExportDTO(
        int schemaVersion,
        Instant exportedAt,
        ExportUser user,
        List<ExportWorkout> workouts,
        List<ExportPlan> plans,
        List<ExportExercise> exercises,
        List<ExportRoutine> routines,
        List<ExportStrengthSession> strengthSessions,
        List<ExportRecipe> recipes,
        List<ExportDietPlan> dietPlans
) {

    /** Versión actual del formato. Súbela al cambiar la estructura. */
    public static final int CURRENT_SCHEMA_VERSION = 4;

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

    public record ExportExercise(String name, String muscleGroup, String equipment, String description) {
    }

    public record ExportRoutine(
            String name,
            String description,
            Instant createdAt,
            List<ExportRoutineItem> items
    ) {
    }

    public record ExportRoutineItem(
            String exerciseName,
            Integer sets,
            Integer reps,
            Integer restSeconds,
            String notes
    ) {
    }

    public record ExportStrengthSession(
            LocalDate date,
            String routineName,
            String notes,
            Instant createdAt
    ) {
    }

    public record ExportRecipe(
            String name,
            String description,
            Integer servings,
            Double calories,
            Double protein,
            Double carbs,
            Double fat,
            String steps,
            Instant createdAt,
            List<ExportIngredient> ingredients
    ) {
    }

    public record ExportIngredient(String name, Double quantity, String unit) {
    }

    public record ExportDietPlan(
            String name,
            LocalDate startDate,
            LocalDate endDate,
            Double targetCalories,
            Double targetProtein,
            Double targetCarbs,
            Double targetFat,
            String notes,
            Instant createdAt,
            List<ExportMeal> meals
    ) {
    }

    public record ExportMeal(LocalDate date, MealType mealType, String recipeName, String notes) {
    }
}
