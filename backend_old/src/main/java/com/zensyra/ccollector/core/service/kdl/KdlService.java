package com.zensyra.ccollector.core.service.kdl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zensyra.ccollector.core.domain.auth.CollectorUser;
import com.zensyra.ccollector.core.dto.gym.ExerciseRequest;
import com.zensyra.ccollector.core.dto.gym.RoutineRequest;
import com.zensyra.ccollector.core.dto.kdl.KdlDocument;
import com.zensyra.ccollector.core.dto.kdl.KdlImportResult;
import com.zensyra.ccollector.core.dto.kdl.KdlItem;
import com.zensyra.ccollector.core.dto.nutrition.DietPlanRequest;
import com.zensyra.ccollector.core.dto.nutrition.IngredientRequest;
import com.zensyra.ccollector.core.dto.nutrition.RecipeRequest;
import com.zensyra.ccollector.core.dto.activity.DailyActivityRequest;
import com.zensyra.ccollector.core.dto.plan.BlockRequest;
import com.zensyra.ccollector.core.dto.plan.PlanRequest;
import com.zensyra.ccollector.core.dto.plan.PlannedSessionRequest;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO;
import com.zensyra.ccollector.core.dto.template.SessionTemplateRequest;
import com.zensyra.ccollector.core.dto.weight.WeightEntryRequest;
import com.zensyra.ccollector.core.dto.workout.WorkoutRequest;
import com.zensyra.ccollector.core.service.gym.ExerciseService;
import com.zensyra.ccollector.core.service.gym.RoutineService;
import com.zensyra.ccollector.core.service.nutrition.DietPlanService;
import com.zensyra.ccollector.core.service.nutrition.IngredientService;
import com.zensyra.ccollector.core.service.nutrition.RecipeService;
import com.zensyra.ccollector.core.service.plan.PlanService;
import com.zensyra.ccollector.core.service.plan.PlannedSessionService;
import com.zensyra.ccollector.core.service.plan.TrainingBlockService;
import com.zensyra.ccollector.core.service.activity.DailyActivityService;
import com.zensyra.ccollector.core.service.session.SessionService;
import com.zensyra.ccollector.core.service.template.SessionTemplateService;
import com.zensyra.ccollector.core.service.weight.WeightService;
import com.zensyra.ccollector.core.service.workout.WorkoutService;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kore Data Language (KDL): import/export de recursos sueltos (o en lote) con un
 * envoltorio común {@code {koreType, schemaVersion, payload}}.
 *
 * <p>El <b>export</b> reutiliza {@link SessionService#export} (payloads ya sin
 * ids de BD) y los reempaqueta como items. El <b>import</b> convierte cada
 * payload al {@code *Request} del recurso y lo crea con su servicio (que valida)
 * — así se reutiliza toda la lógica existente. Campos desconocidos se ignoran
 * (mapper tolerante), lo que da compatibilidad entre versiones para cambios
 * aditivos; para cambios de fondo, cada tipo tendrá su migrador por
 * {@code schemaVersion} (hoy todos en v1, migración identidad).
 */
@ApplicationScoped
public class KdlService {

    /** Versión de esquema actual por tipo de recurso soportado. */
    private static final Map<String, Integer> VERSIONS = Map.ofEntries(
            Map.entry("workout", 1),
            Map.entry("plan", 1),
            Map.entry("planned", 1),
            Map.entry("block", 1),
            Map.entry("exercise", 1),
            Map.entry("ingredient", 1),
            Map.entry("recipe", 1),
            Map.entry("routine", 1),
            Map.entry("dietPlan", 1),
            Map.entry("weightEntry", 1),
            Map.entry("sessionTemplate", 1),
            Map.entry("dailyActivity", 1));

    private final ObjectMapper mapper;
    private final SessionService sessions;
    private final WorkoutService workouts;
    private final PlanService plans;
    private final PlannedSessionService plannedSessions;
    private final TrainingBlockService trainingBlocks;
    private final ExerciseService exercises;
    private final IngredientService ingredients;
    private final RecipeService recipes;
    private final RoutineService routines;
    private final DietPlanService dietPlans;
    private final WeightService weight;
    private final SessionTemplateService templates;
    private final DailyActivityService dailyActivity;

    public KdlService(ObjectMapper mapper, SessionService sessions, WorkoutService workouts,
                      PlanService plans, PlannedSessionService plannedSessions,
                      TrainingBlockService trainingBlocks,
                      ExerciseService exercises, IngredientService ingredients,
                      RecipeService recipes, RoutineService routines, DietPlanService dietPlans,
                      WeightService weight, SessionTemplateService templates,
                      DailyActivityService dailyActivity) {
        this.mapper = mapper;
        this.sessions = sessions;
        this.workouts = workouts;
        this.plans = plans;
        this.plannedSessions = plannedSessions;
        this.trainingBlocks = trainingBlocks;
        this.exercises = exercises;
        this.ingredients = ingredients;
        this.recipes = recipes;
        this.routines = routines;
        this.dietPlans = dietPlans;
        this.weight = weight;
        this.templates = templates;
        this.dailyActivity = dailyActivity;
    }

    /** Exporta recursos como documento KDL. {@code types} vacío = todos. */
    public KdlDocument export(CollectorUser user, Set<String> types) {
        SessionExportDTO d = sessions.export(user);
        List<KdlItem> items = new ArrayList<>();
        add(items, types, "workout", d.workouts());
        add(items, types, "plan", d.plans());
        add(items, types, "planned", d.calendarSessions());
        add(items, types, "block", d.trainingBlocks());
        add(items, types, "exercise", d.exercises());
        add(items, types, "ingredient", d.ingredients());
        add(items, types, "recipe", d.recipes());
        add(items, types, "routine", d.routines());
        add(items, types, "dietPlan", d.dietPlans());
        add(items, types, "weightEntry", d.weightEntries());
        add(items, types, "sessionTemplate", d.sessionTemplates());
        add(items, types, "dailyActivity", d.dailyActivities());
        return new KdlDocument(KdlDocument.FORMAT, KdlDocument.CURRENT_VERSION, items);
    }

    private void add(List<KdlItem> items, Set<String> types, String type, List<?> payloads) {
        if (payloads == null || (types != null && !types.isEmpty() && !types.contains(type))) {
            return;
        }
        int version = VERSIONS.get(type);
        for (Object payload : payloads) {
            items.add(new KdlItem(type, version, mapper.valueToTree(payload)));
        }
    }

    /**
     * Import "por sección": trata el cuerpo como uno o varios recursos de un
     * único {@code type} (el mismo cuerpo que su POST). Acepta tres formas —
     * (1) un documento KDL completo (se respeta tal cual), (2) un array de
     * payloads, (3) un único payload — para que "pide a una IA un plan de 8
     * semanas" se importe directo en su sección sin envoltorio.
     */
    public KdlImportResult importAs(Long userId, String type, JsonNode body) {
        if (type == null || !VERSIONS.containsKey(type)) {
            return new KdlImportResult(0, 0, List.of("tipo '" + type + "' no reconocido"), Map.of());
        }
        if (body == null || body.isNull()) {
            return new KdlImportResult(0, 0, List.of("cuerpo vacío"), Map.of());
        }
        // Si ya es un documento KDL (trae items), respétalo tal cual.
        if (body.has("items") && body.get("items").isArray()) {
            try {
                return importDoc(userId, mapper.treeToValue(body, KdlDocument.class));
            } catch (Exception e) {
                return new KdlImportResult(0, 0, List.of("documento KDL inválido: " + e.getMessage()), Map.of());
            }
        }
        int version = VERSIONS.get(type);
        List<KdlItem> items = new ArrayList<>();
        if (body.isArray()) {
            for (JsonNode el : body) {
                items.add(new KdlItem(type, version, el));
            }
        } else {
            items.add(new KdlItem(type, version, body));
        }
        return importDoc(userId, new KdlDocument(KdlDocument.FORMAT, KdlDocument.CURRENT_VERSION, items));
    }

    /**
     * Importa un documento KDL en la sesión del usuario. No es transaccional a
     * nivel de lote: cada recurso se crea en su propia transacción (servicio),
     * de modo que un item inválido no revierte los ya importados.
     */
    public KdlImportResult importDoc(Long userId, KdlDocument doc) {
        if (doc == null || doc.items() == null || doc.items().isEmpty()) {
            return new KdlImportResult(0, 0, List.of("Documento KDL vacío o sin items"), Map.of());
        }
        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        Map<String, Integer> byType = new LinkedHashMap<>();

        int index = 0;
        for (KdlItem item : doc.items()) {
            index++;
            String type = item == null ? null : item.koreType();
            try {
                if (item == null || type == null || type.isBlank()) {
                    throw new IllegalArgumentException("item sin koreType");
                }
                Integer current = VERSIONS.get(type);
                if (current == null) {
                    throw new IllegalArgumentException("tipo '" + type + "' no reconocido");
                }
                int version = item.schemaVersion() <= 0 ? current : item.schemaVersion();
                if (version > current) {
                    throw new IllegalArgumentException(
                            "schemaVersion " + version + " no soportado para '" + type + "' (máx " + current + ")");
                }
                importItem(userId, type, migrate(type, version, item.payload()));
                imported++;
                byType.merge(type, 1, Integer::sum);
            } catch (Exception e) {
                skipped++;
                errors.add("item #" + index + " (" + (type == null ? "?" : type) + "): " + e.getMessage());
            }
        }
        return new KdlImportResult(imported, skipped, errors, byType);
    }

    /**
     * Migra un payload de una versión antigua a la actual del tipo. Hoy todos los
     * tipos están en v1 (identidad); aquí se enganchan futuros migradores.
     */
    private JsonNode migrate(String type, int fromVersion, JsonNode payload) {
        return payload;
    }

    private void importItem(Long userId, String type, JsonNode payload) throws Exception {
        switch (type) {
            case "workout" -> workouts.create(userId, mapper.treeToValue(payload, WorkoutRequest.class));
            case "plan" -> plans.create(userId, mapper.treeToValue(payload, PlanRequest.class));
            case "planned" -> plannedSessions.create(userId, mapper.treeToValue(payload, PlannedSessionRequest.class));
            case "block" -> trainingBlocks.create(userId, mapper.treeToValue(payload, BlockRequest.class));
            case "exercise" -> exercises.create(userId, mapper.treeToValue(payload, ExerciseRequest.class));
            case "ingredient" -> ingredients.create(userId, mapper.treeToValue(payload, IngredientRequest.class));
            case "recipe" -> recipes.create(userId, mapper.treeToValue(payload, RecipeRequest.class));
            case "routine" -> routines.create(userId, mapper.treeToValue(payload, RoutineRequest.class));
            case "dietPlan" -> dietPlans.create(userId, mapper.treeToValue(payload, DietPlanRequest.class));
            case "weightEntry" -> weight.upsert(userId, mapper.treeToValue(payload, WeightEntryRequest.class));
            case "sessionTemplate" -> templates.create(userId, mapper.treeToValue(payload, SessionTemplateRequest.class));
            case "dailyActivity" -> dailyActivity.upsert(userId, mapper.treeToValue(payload, DailyActivityRequest.class));
            default -> throw new IllegalArgumentException("tipo '" + type + "' no soportado");
        }
    }
}
