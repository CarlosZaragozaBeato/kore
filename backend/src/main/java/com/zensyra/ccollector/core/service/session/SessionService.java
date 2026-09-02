package com.zensyra.ccollector.core.service.session;

import com.zensyra.ccollector.core.domain.activity.DailyActivity;
import com.zensyra.ccollector.core.domain.auth.CollectorUser;
import com.zensyra.ccollector.core.domain.gym.Exercise;
import com.zensyra.ccollector.core.domain.gym.Routine;
import com.zensyra.ccollector.core.domain.gym.RoutineItem;
import com.zensyra.ccollector.core.domain.gym.StrengthSession;
import com.zensyra.ccollector.core.domain.gym.StrengthStatus;
import com.zensyra.ccollector.core.domain.nutrition.BaseUnit;
import com.zensyra.ccollector.core.domain.nutrition.DietMeal;
import com.zensyra.ccollector.core.domain.nutrition.DietPlan;
import com.zensyra.ccollector.core.domain.nutrition.Ingredient;
import com.zensyra.ccollector.core.domain.nutrition.Recipe;
import com.zensyra.ccollector.core.domain.nutrition.RecipeIngredient;
import com.zensyra.ccollector.core.domain.template.SessionTemplate;
import com.zensyra.ccollector.core.domain.weight.WeightEntry;
import com.zensyra.ccollector.core.domain.weight.WeightGoal;
import com.zensyra.ccollector.core.domain.plan.BlockFocus;
import com.zensyra.ccollector.core.domain.plan.PlannedSession;
import com.zensyra.ccollector.core.domain.plan.PlannedStep;
import com.zensyra.ccollector.core.domain.plan.SessionStatus;
import com.zensyra.ccollector.core.domain.plan.StepKind;
import com.zensyra.ccollector.core.domain.plan.TrainingBlock;
import com.zensyra.ccollector.core.domain.plan.TrainingPlan;
import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.domain.workout.WorkoutSource;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportCatalogIngredient;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportExercise;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportPlan;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportBlock;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportCalendarSession;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportPlannedSession;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportPlannedStep;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportRoutine;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportRoutineItem;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportStrengthSession;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportDietPlan;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportIngredient;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportMeal;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportDailyActivity;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportRecipe;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportSessionTemplate;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportUser;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportWeightEntry;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportWeightGoal;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO.ExportWorkout;
import com.zensyra.ccollector.core.repository.auth.UserRepository;
import com.zensyra.ccollector.core.repository.gym.ExerciseRepository;
import com.zensyra.ccollector.core.repository.gym.RoutineItemRepository;
import com.zensyra.ccollector.core.repository.gym.RoutineRepository;
import com.zensyra.ccollector.core.repository.gym.StrengthSessionRepository;
import com.zensyra.ccollector.core.repository.nutrition.DietMealRepository;
import com.zensyra.ccollector.core.repository.activity.DailyActivityRepository;
import com.zensyra.ccollector.core.repository.nutrition.DietPlanRepository;
import com.zensyra.ccollector.core.repository.nutrition.IngredientRepository;
import com.zensyra.ccollector.core.repository.nutrition.RecipeIngredientRepository;
import com.zensyra.ccollector.core.repository.nutrition.RecipeRepository;
import com.zensyra.ccollector.core.repository.template.SessionTemplateRepository;
import com.zensyra.ccollector.core.repository.weight.WeightEntryRepository;
import com.zensyra.ccollector.core.repository.weight.WeightGoalRepository;
import com.zensyra.ccollector.core.repository.plan.PlanRepository;
import com.zensyra.ccollector.core.repository.plan.PlannedSessionRepository;
import com.zensyra.ccollector.core.repository.plan.PlannedStepRepository;
import com.zensyra.ccollector.core.repository.plan.TrainingBlockRepository;
import com.zensyra.ccollector.core.repository.workout.WorkoutRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class SessionService {

    private final UserRepository users;
    private final WorkoutRepository workouts;
    private final PlanRepository plans;
    private final PlannedSessionRepository plannedSessions;
    private final PlannedStepRepository plannedSteps;
    private final ExerciseRepository exercises;
    private final RoutineRepository routines;
    private final RoutineItemRepository routineItems;
    private final StrengthSessionRepository strengthSessions;
    private final RecipeRepository recipes;
    private final RecipeIngredientRepository recipeIngredients;
    private final DietPlanRepository dietPlans;
    private final DietMealRepository dietMeals;
    private final IngredientRepository ingredients;
    private final WeightEntryRepository weightEntries;
    private final WeightGoalRepository weightGoals;
    private final SessionTemplateRepository sessionTemplates;
    private final DailyActivityRepository dailyActivities;
    private final TrainingBlockRepository trainingBlocks;

    public SessionService(UserRepository users, WorkoutRepository workouts,
                          PlanRepository plans, PlannedSessionRepository plannedSessions,
                          PlannedStepRepository plannedSteps,
                          ExerciseRepository exercises, RoutineRepository routines,
                          RoutineItemRepository routineItems, StrengthSessionRepository strengthSessions,
                          RecipeRepository recipes, RecipeIngredientRepository recipeIngredients,
                          DietPlanRepository dietPlans, DietMealRepository dietMeals,
                          IngredientRepository ingredients,
                          WeightEntryRepository weightEntries, WeightGoalRepository weightGoals,
                          SessionTemplateRepository sessionTemplates,
                          DailyActivityRepository dailyActivities,
                          TrainingBlockRepository trainingBlocks) {
        this.users = users;
        this.workouts = workouts;
        this.plans = plans;
        this.plannedSessions = plannedSessions;
        this.plannedSteps = plannedSteps;
        this.exercises = exercises;
        this.routines = routines;
        this.routineItems = routineItems;
        this.strengthSessions = strengthSessions;
        this.recipes = recipes;
        this.recipeIngredients = recipeIngredients;
        this.dietPlans = dietPlans;
        this.dietMeals = dietMeals;
        this.ingredients = ingredients;
        this.weightEntries = weightEntries;
        this.weightGoals = weightGoals;
        this.sessionTemplates = sessionTemplates;
        this.dailyActivities = dailyActivities;
        this.trainingBlocks = trainingBlocks;
    }

    /** Vuelca la sesión completa del usuario a un documento portable. */
    public SessionExportDTO export(CollectorUser user) {
        var exportWorkouts = workouts.listByUser(user.id).stream()
                .map(w -> new ExportWorkout(
                        w.date, w.type, w.distanceMeters, w.durationSeconds,
                        w.avgHeartRate, w.maxHeartRate, w.energyKcal, w.stepCount, w.perceivedEffort,
                        w.notes, w.source, w.createdAt))
                .toList();
        var exportPlans = plans.listByUser(user.id).stream()
                .map(p -> new ExportPlan(
                        p.name, p.goal, p.startDate, p.endDate, p.createdAt,
                        plannedSessions.listByPlan(p.id).stream()
                                .map(s -> new ExportPlannedSession(
                                        s.date, s.type, s.targetDistanceMeters,
                                        s.targetDurationSeconds, s.description,
                                        plannedSteps.listBySession(s.id).stream()
                                                .map(st -> new ExportPlannedStep(
                                                        st.kind, st.repeat, st.targetDistanceMeters,
                                                        st.targetDurationSeconds, st.targetPaceMinSecPerKm,
                                                        st.targetPaceMaxSecPerKm, st.targetHrMin, st.targetHrMax,
                                                        st.recoverySeconds, st.note))
                                                .toList()))
                                .toList()))
                .toList();
        var exportExercises = exercises.listByUser(user.id).stream()
                .map(e -> new ExportExercise(e.name, e.muscleGroup, e.category,
                        e.requiresEquipment, e.equipment, e.description,
                        e.imageUrl, e.instructions, e.metValue))
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
                .map(s -> new ExportStrengthSession(s.date, s.routineName, s.notes,
                        s.status != null ? s.status.name() : null, s.createdAt))
                .toList();
        var exportRecipes = recipes.listByUser(user.id).stream()
                .map(r -> new ExportRecipe(
                        r.name, r.description, r.servings, r.calories, r.protein, r.carbs, r.fat,
                        r.steps, r.createdAt,
                        recipeIngredients.listByRecipe(r.id).stream()
                                .map(i -> new ExportIngredient(i.name, i.quantity, i.unit, i.ingredientId))
                                .toList()))
                .toList();
        var exportCatalogIngredients = ingredients.listByUser(user.id).stream()
                .map(i -> new ExportCatalogIngredient(i.name, i.baseUnit, i.imageUrl, i.calories, i.protein,
                        i.carbs, i.fat, i.fiber, i.sugars, i.sodium))
                .toList();
        var exportWeight = weightEntries.listByUser(user.id).stream()
                .map(e -> new ExportWeightEntry(e.date, e.weightKg))
                .toList();
        WeightGoal goal = weightGoals.findByUser(user.id).orElse(null);
        ExportWeightGoal exportGoal = goal == null ? null
                : new ExportWeightGoal(goal.minKg, goal.maxKg, goal.maintenanceKcal);
        var exportTemplates = sessionTemplates.listByUser(user.id).stream()
                .map(t -> new ExportSessionTemplate(t.name, t.discipline, t.goal, t.level,
                        t.targetDistanceMeters, t.targetDurationSeconds, t.structure, t.notes))
                .toList();
        var exportActivities = dailyActivities.listByUser(user.id).stream()
                .map(a -> new ExportDailyActivity(a.date, a.steps, a.burnedKcal))
                .toList();
        var exportCalendar = plannedSessions.listStandaloneByUser(user.id).stream()
                .map(s -> new ExportCalendarSession(
                        s.date, s.type, s.targetDistanceMeters, s.targetDurationSeconds, s.description,
                        s.status, s.variantGroup, s.variantLabel,
                        plannedSteps.listBySession(s.id).stream()
                                .map(st -> new ExportPlannedStep(
                                        st.kind, st.repeat, st.targetDistanceMeters,
                                        st.targetDurationSeconds, st.targetPaceMinSecPerKm,
                                        st.targetPaceMaxSecPerKm, st.targetHrMin, st.targetHrMax,
                                        st.recoverySeconds, st.note))
                                .toList()))
                .toList();
        var exportDiets = dietPlans.listByUser(user.id).stream()
                .map(d -> new ExportDietPlan(
                        d.name, d.startDate, d.endDate, d.targetCalories, d.targetProtein,
                        d.targetCarbs, d.targetFat, d.notes, d.createdAt,
                        dietMeals.listByPlan(d.id).stream()
                                .map(m -> new ExportMeal(m.date, m.mealType, m.recipeName, m.notes))
                                .toList()))
                .toList();

        var allBlocks = trainingBlocks.listByUser(user.id);
        var blocksByParent = allBlocks.stream()
                .filter(b -> b.parentId != null)
                .collect(Collectors.groupingBy(b -> b.parentId));
        var exportBlocks = allBlocks.stream()
                .filter(b -> b.parentId == null)
                .map(b -> toExportBlock(b, blocksByParent))
                .toList();

        return new SessionExportDTO(
                SessionExportDTO.CURRENT_SCHEMA_VERSION,
                Instant.now(),
                new ExportUser(user.username, user.createdAt),
                exportWorkouts,
                exportPlans,
                exportExercises,
                exportRoutines,
                exportStrength,
                exportRecipes,
                exportDiets,
                exportCatalogIngredients,
                exportWeight,
                exportGoal,
                exportTemplates,
                exportActivities,
                exportCalendar,
                exportBlocks);
    }

    /** Reconstruye recursivamente el árbol de bloques (macro→meso→micro) para exportar. */
    private ExportBlock toExportBlock(TrainingBlock b, Map<Long, List<TrainingBlock>> byParent) {
        List<ExportBlock> children = byParent.getOrDefault(b.id, List.of()).stream()
                .map(c -> toExportBlock(c, byParent))
                .toList();
        return new ExportBlock(b.level, b.focus, b.name, b.startDate, b.endDate,
                b.loadStance, b.note, children);
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
                w.maxHeartRate = ew.maxHeartRate();
                w.energyKcal = ew.energyKcal();
                w.stepCount = ew.stepCount();
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
                        s.userId = user.id;
                        s.status = SessionStatus.ACCEPTED;
                        s.date = es.date();
                        s.type = es.type();
                        s.targetDistanceMeters = es.targetDistanceMeters();
                        s.targetDurationSeconds = es.targetDurationSeconds();
                        s.description = es.description();
                        plannedSessions.persist(s);
                        importSteps(s.id, es.steps());
                    }
                }
            }
        }

        if (doc.calendarSessions() != null) {
            for (ExportCalendarSession ec : doc.calendarSessions()) {
                PlannedSession s = new PlannedSession();
                s.planId = null;
                s.userId = user.id;
                s.date = ec.date();
                s.type = ec.type();
                s.targetDistanceMeters = ec.targetDistanceMeters();
                s.targetDurationSeconds = ec.targetDurationSeconds();
                s.description = ec.description();
                s.status = ec.status() != null ? ec.status() : SessionStatus.PROPOSED;
                s.variantGroup = ec.variantGroup();
                s.variantLabel = ec.variantLabel();
                plannedSessions.persist(s);
                importSteps(s.id, ec.steps());
            }
        }

        if (doc.trainingBlocks() != null) {
            for (ExportBlock eb : doc.trainingBlocks()) {
                importBlock(user.id, eb, null);
            }
        }

        if (doc.exercises() != null) {
            for (ExportExercise ee : doc.exercises()) {
                Exercise e = new Exercise();
                e.userId = user.id;
                e.name = ee.name();
                e.muscleGroup = ee.muscleGroup();
                e.category = ee.category();
                e.requiresEquipment = ee.requiresEquipment();
                e.equipment = ee.equipment();
                e.description = ee.description();
                e.imageUrl = ee.imageUrl();
                e.instructions = ee.instructions();
                e.metValue = ee.metValue();
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
                s.status = parseStrengthStatus(es.status());
                s.createdAt = es.createdAt() != null ? es.createdAt() : Instant.now();
                strengthSessions.persist(s);
            }
        }

        if (doc.recipes() != null) {
            for (ExportRecipe er : doc.recipes()) {
                Recipe recipe = new Recipe();
                recipe.userId = user.id;
                recipe.name = er.name();
                recipe.description = er.description();
                recipe.servings = er.servings();
                recipe.calories = er.calories();
                recipe.protein = er.protein();
                recipe.carbs = er.carbs();
                recipe.fat = er.fat();
                recipe.steps = er.steps();
                recipe.createdAt = er.createdAt() != null ? er.createdAt() : Instant.now();
                recipes.persist(recipe);
                if (er.ingredients() != null) {
                    int position = 0;
                    for (ExportIngredient ei : er.ingredients()) {
                        RecipeIngredient ingredient = new RecipeIngredient();
                        ingredient.recipeId = recipe.id;
                        ingredient.position = position++;
                        ingredient.name = ei.name();
                        ingredient.quantity = ei.quantity();
                        ingredient.unit = ei.unit();
                        ingredient.ingredientId = ei.ingredientId();
                        recipeIngredients.persist(ingredient);
                    }
                }
            }
        }

        if (doc.dietPlans() != null) {
            for (ExportDietPlan ed : doc.dietPlans()) {
                DietPlan plan = new DietPlan();
                plan.userId = user.id;
                plan.name = ed.name();
                plan.startDate = ed.startDate();
                plan.endDate = ed.endDate();
                plan.targetCalories = ed.targetCalories();
                plan.targetProtein = ed.targetProtein();
                plan.targetCarbs = ed.targetCarbs();
                plan.targetFat = ed.targetFat();
                plan.notes = ed.notes();
                plan.createdAt = ed.createdAt() != null ? ed.createdAt() : Instant.now();
                dietPlans.persist(plan);
                if (ed.meals() != null) {
                    for (ExportMeal em : ed.meals()) {
                        DietMeal meal = new DietMeal();
                        meal.dietPlanId = plan.id;
                        meal.date = em.date();
                        meal.mealType = em.mealType();
                        meal.recipeName = em.recipeName();
                        meal.notes = em.notes();
                        dietMeals.persist(meal);
                    }
                }
            }
        }

        if (doc.ingredients() != null) {
            for (ExportCatalogIngredient ci : doc.ingredients()) {
                Ingredient ingredient = new Ingredient();
                ingredient.userId = user.id;
                ingredient.name = ci.name();
                ingredient.baseUnit = ci.baseUnit() != null ? ci.baseUnit() : BaseUnit.GRAM;
                ingredient.imageUrl = ci.imageUrl();
                ingredient.calories = ci.calories();
                ingredient.protein = ci.protein();
                ingredient.carbs = ci.carbs();
                ingredient.fat = ci.fat();
                ingredient.fiber = ci.fiber();
                ingredient.sugars = ci.sugars();
                ingredient.sodium = ci.sodium();
                ingredients.persist(ingredient);
            }
        }

        if (doc.weightEntries() != null) {
            for (ExportWeightEntry we : doc.weightEntries()) {
                WeightEntry entry = new WeightEntry();
                entry.userId = user.id;
                entry.date = we.date();
                entry.weightKg = we.weightKg();
                weightEntries.persist(entry);
            }
        }
        if (doc.weightGoal() != null) {
            WeightGoal goal = new WeightGoal();
            goal.userId = user.id;
            goal.minKg = doc.weightGoal().minKg();
            goal.maxKg = doc.weightGoal().maxKg();
            goal.maintenanceKcal = doc.weightGoal().maintenanceKcal();
            weightGoals.persist(goal);
        }

        if (doc.dailyActivities() != null) {
            for (ExportDailyActivity da : doc.dailyActivities()) {
                DailyActivity a = new DailyActivity();
                a.userId = user.id;
                a.date = da.date();
                a.steps = da.steps();
                a.burnedKcal = da.burnedKcal();
                dailyActivities.persist(a);
            }
        }

        if (doc.sessionTemplates() != null) {
            for (ExportSessionTemplate et : doc.sessionTemplates()) {
                SessionTemplate t = new SessionTemplate();
                t.userId = user.id;
                t.name = et.name();
                t.discipline = et.discipline();
                t.goal = et.goal();
                t.level = et.level();
                t.targetDistanceMeters = et.targetDistanceMeters();
                t.targetDurationSeconds = et.targetDurationSeconds();
                t.structure = et.structure();
                t.notes = et.notes();
                sessionTemplates.persist(t);
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

    /** Recrea los pasos estructurados de una sesión planificada importada. */
    private void importSteps(Long sessionId, List<ExportPlannedStep> exportSteps) {
        if (exportSteps == null) {
            return;
        }
        int order = 0;
        for (ExportPlannedStep est : exportSteps) {
            PlannedStep step = new PlannedStep();
            step.plannedSessionId = sessionId;
            step.orderIndex = order++;
            step.kind = est.kind() != null ? est.kind() : StepKind.STEADY;
            step.repeat = est.repeat() < 1 ? 1 : est.repeat();
            step.targetDistanceMeters = est.targetDistanceMeters();
            step.targetDurationSeconds = est.targetDurationSeconds();
            step.targetPaceMinSecPerKm = est.targetPaceMinSecPerKm();
            step.targetPaceMaxSecPerKm = est.targetPaceMaxSecPerKm();
            step.targetHrMin = est.targetHrMin();
            step.targetHrMax = est.targetHrMax();
            step.recoverySeconds = est.recoverySeconds();
            step.note = est.note();
            plannedSteps.persist(step);
        }
    }

    /** Recrea recursivamente un bloque de periodización y sus hijos, asignando parentId. */
    private void importBlock(Long userId, ExportBlock eb, Long parentId) {
        TrainingBlock b = new TrainingBlock();
        b.userId = userId;
        b.parentId = parentId;
        b.level = eb.level();
        b.focus = eb.focus() != null ? eb.focus() : BlockFocus.GENERAL;
        b.name = eb.name();
        b.startDate = eb.startDate();
        b.endDate = eb.endDate();
        b.loadStance = eb.loadStance();
        b.note = eb.note();
        trainingBlocks.persist(b);
        if (eb.children() != null) {
            for (ExportBlock child : eb.children()) {
                importBlock(userId, child, b.id);
            }
        }
    }

    /** Documentos previos a v11 no traen estado; se asumen realizadas (DONE). */
    private static StrengthStatus parseStrengthStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return StrengthStatus.DONE;
        }
        try {
            return StrengthStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return StrengthStatus.DONE;
        }
    }
}
