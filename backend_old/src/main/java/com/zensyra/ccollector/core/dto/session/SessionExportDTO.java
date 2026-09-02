package com.zensyra.ccollector.core.dto.session;

import com.zensyra.ccollector.core.domain.gym.ExerciseCategory;
import com.zensyra.ccollector.core.domain.nutrition.BaseUnit;
import com.zensyra.ccollector.core.domain.nutrition.MealType;
import com.zensyra.ccollector.core.domain.plan.BlockFocus;
import com.zensyra.ccollector.core.domain.plan.BlockLevel;
import com.zensyra.ccollector.core.domain.plan.LoadStance;
import com.zensyra.ccollector.core.domain.plan.SessionStatus;
import com.zensyra.ccollector.core.domain.plan.StepKind;
import com.zensyra.ccollector.core.domain.template.Level;
import com.zensyra.ccollector.core.domain.template.RaceGoal;
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
 * rutinas, sesiones de fuerza). v4: añade nutrición (recetas, dietas). v5:
 * workouts con FC máx y kcal. v6: catálogo de ingredientes, categoría/equipo en
 * ejercicios y enlace ingrediente-catálogo en recetas. v7: registro de peso y
 * objetivo de mantenimiento. v8: plantillas de sesión específicas. v9: pasos
 * en workouts. v10: actividad diaria (pasos y quema). v11: ejercicios con
 * imagen/instrucciones/MET y estado en las sesiones de fuerza (planificadas).
 * v12: imagen en el catálogo de ingredientes. v13: pasos estructurados en las
 * sesiones planificadas (calentamiento/serie/recuperación/rodaje/vuelta a la
 * calma, con distancia/tiempo, ritmo min/max, FC, repeticiones y descanso).
 * v14: sesiones planificadas sueltas del calendario (`calendarSessions`): sin
 * plan, con estado (propuesta/aceptada/descartada) y variantes por día.
 * v15: bloques de periodización (`trainingBlocks`): árbol macro/meso/micro con
 * foco, rango de fechas y postura de carga; los hijos viajan anidados.
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
        List<ExportDietPlan> dietPlans,
        List<ExportCatalogIngredient> ingredients,
        List<ExportWeightEntry> weightEntries,
        ExportWeightGoal weightGoal,
        List<ExportSessionTemplate> sessionTemplates,
        List<ExportDailyActivity> dailyActivities,
        List<ExportCalendarSession> calendarSessions,
        List<ExportBlock> trainingBlocks
) {

    /** Versión actual del formato. Súbela al cambiar la estructura. */
    public static final int CURRENT_SCHEMA_VERSION = 15;

    public record ExportUser(String username, Instant createdAt) {
    }

    public record ExportWorkout(
            LocalDate date,
            WorkoutType type,
            Double distanceMeters,
            Long durationSeconds,
            Integer avgHeartRate,
            Integer maxHeartRate,
            Double energyKcal,
            Integer stepCount,
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
            String description,
            List<ExportPlannedStep> steps
    ) {
    }

    public record ExportPlannedStep(
            StepKind kind,
            int repeat,
            Double targetDistanceMeters,
            Long targetDurationSeconds,
            Integer targetPaceMinSecPerKm,
            Integer targetPaceMaxSecPerKm,
            Integer targetHrMin,
            Integer targetHrMax,
            Long recoverySeconds,
            String note
    ) {
    }

    /** Sesión planificada suelta del calendario (sin plan), con estado y variante. */
    public record ExportCalendarSession(
            LocalDate date,
            WorkoutType type,
            Double targetDistanceMeters,
            Long targetDurationSeconds,
            String description,
            SessionStatus status,
            String variantGroup,
            String variantLabel,
            List<ExportPlannedStep> steps
    ) {
    }

    /** Bloque de periodización con sus hijos anidados (macro→meso→micro). */
    public record ExportBlock(
            BlockLevel level,
            BlockFocus focus,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            LoadStance loadStance,
            String note,
            List<ExportBlock> children
    ) {
    }

    public record ExportExercise(
            String name,
            String muscleGroup,
            ExerciseCategory category,
            Boolean requiresEquipment,
            String equipment,
            String description,
            String imageUrl,
            String instructions,
            Double metValue
    ) {
    }

    public record ExportCatalogIngredient(
            String name,
            BaseUnit baseUnit,
            String imageUrl,
            Double calories,
            Double protein,
            Double carbs,
            Double fat,
            Double fiber,
            Double sugars,
            Double sodium
    ) {
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
            String status,
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

    public record ExportIngredient(String name, Double quantity, String unit, Long ingredientId) {
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

    public record ExportWeightEntry(LocalDate date, Double weightKg) {
    }

    public record ExportWeightGoal(Double minKg, Double maxKg, Double maintenanceKcal) {
    }

    public record ExportDailyActivity(LocalDate date, Integer steps, Double burnedKcal) {
    }

    public record ExportSessionTemplate(
            String name,
            WorkoutType discipline,
            RaceGoal goal,
            Level level,
            Double targetDistanceMeters,
            Long targetDurationSeconds,
            String structure,
            String notes
    ) {
    }
}
