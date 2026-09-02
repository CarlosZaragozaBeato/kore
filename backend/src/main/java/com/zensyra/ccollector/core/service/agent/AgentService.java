package com.zensyra.ccollector.core.service.agent;

import com.zensyra.ccollector.core.domain.auth.CollectorUser;
import com.zensyra.ccollector.core.dto.agent.AgentContextDTO;
import com.zensyra.ccollector.core.dto.agent.AgentManifestDTO;
import com.zensyra.ccollector.core.dto.agent.AgentManifestDTO.Docs;
import com.zensyra.ccollector.core.dto.agent.AgentManifestDTO.Io;
import com.zensyra.ccollector.core.dto.agent.AgentManifestDTO.ResourceDescriptor;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO;
import com.zensyra.ccollector.core.service.analytics.AnalyticsService;
import com.zensyra.ccollector.core.service.session.SessionService;
import com.zensyra.ccollector.core.session.CurrentSession;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AgentService {

    private final SessionService sessions;
    private final AnalyticsService analytics;

    public AgentService(SessionService sessions, AnalyticsService analytics) {
        this.sessions = sessions;
        this.analytics = analytics;
    }

    public AgentManifestDTO manifest() {
        List<ResourceDescriptor> resources = List.of(
                crud("workouts", "Entrenamientos (running/fuerza/otros); origen MANUAL o SUUNTO", "/api/v1/workouts"),
                crud("plans", "Planes de entrenamiento con sesiones planificadas (pasos estructurados) y adherencia", "/api/v1/plans"),
                crud("planned",
                        "Sesiones planificadas sueltas del calendario (sin plan), con pasos estructurados, "
                                + "estado (PROPOSED/ACCEPTED/REJECTED) y variantes por día; "
                                + "POST /api/v1/planned/{id}/accept y /reject deciden entre variantes; "
                                + "GET /api/v1/planned/{id}/comparison contrasta lo planificado con el "
                                + "entreno realizado del día (distancia/duración/ritmo/FC vs objetivo); "
                                + "GET /api/v1/planned/recommendation sugiere postura de carga "
                                + "(DELOAD/MAINTAIN/BUILD) según ACWR/monotonía/rampa y "
                                + "POST /api/v1/planned/{id}/variants genera variantes de descarga/subida",
                        "/api/v1/planned"),
                crud("blocks",
                        "Bloques de periodización (macro/meso/micro) sobre el calendario; se anidan "
                                + "por parentId y el POST admite hijos anidados (una temporada de una vez); "
                                + "GET /api/v1/blocks/{id}/summary resume planificado vs realizado en el "
                                + "rango del bloque (relación con las sesiones derivada por fechas)",
                        "/api/v1/blocks"),
                crud("exercises",
                        "Catálogo de ejercicios (categoría WARMUP/STRENGTH/RECOVERY, con/sin material); "
                                + "POST /api/v1/exercises/seed carga ejemplos", "/api/v1/exercises"),
                crud("ingredients",
                        "Catálogo de ingredientes con valores nutricionales por 100 g/ml e imagen "
                                + "(imageUrl); POST /api/v1/ingredients/seed carga ejemplos", "/api/v1/ingredients"),
                crud("routines", "Rutinas de fuerza con ejercicios (series/reps/descanso)", "/api/v1/routines"),
                crud("sessionTemplates",
                        "Plantillas de sesión específicas por disciplina/objetivo (maratón, media, 10k, "
                                + "5k)/nivel; POST /api/v1/session-templates/seed carga ejemplos",
                        "/api/v1/session-templates"),
                crud("strengthSessions", "Registro de sesiones de fuerza realizadas", "/api/v1/strength-sessions"),
                crud("recipes", "Recetas con ingredientes, pasos y macros. GET /recipes/recommend "
                        + "sugiere recetas según el contexto (entreno/actividad/objetivo); "
                        + "GET /recipes/by-ingredient/{id} lista las que usan un ingrediente del catálogo",
                        "/api/v1/recipes"),
                crud("dietPlans", "Planes de dieta con objetivos y comidas por día", "/api/v1/diet-plans"),
                new ResourceDescriptor("weight",
                        "Registro de peso (upsert por fecha) y objetivo de mantenimiento (GET/PUT /weight/goal)",
                        "GET /api/v1/weight", null, "POST /api/v1/weight", "PUT /api/v1/weight/goal",
                        "DELETE /api/v1/weight/{id}"),
                new ResourceDescriptor("dailyActivity",
                        "Actividad diaria (pasos y quema total del día, upsert por fecha); la quema "
                                + "diaria sustituye a la estimación por entrenos en el balance",
                        "GET /api/v1/activity/daily", null, "POST /api/v1/activity/daily", null,
                        "DELETE /api/v1/activity/daily/{id}"),
                new ResourceDescriptor("energy",
                        "Balance energético: kcal consumidas vs quemadas, serie diaria y semanal",
                        null, "/api/v1/energy/summary", null, null, null),
                new ResourceDescriptor("suuntoSettings",
                        "Configuración de la integración Suunto (los secretos no se devuelven)",
                        null, "/api/v1/suunto/settings", null, "PUT /api/v1/suunto/settings", null),
                new ResourceDescriptor("context",
                        "Contexto de entrenamiento acotado a un rango para entregar a un agente: "
                                + "GET /api/v1/context/training?from=YYYY-MM-DD&to=YYYY-MM-DD devuelve lo "
                                + "realizado, lo planificado y los bloques que solapan, señales de carga y "
                                + "una guía autodescriptiva con el formato para importar el plan de vuelta "
                                + "(POST /api/v1/kdl/import/planned)",
                        null, "/api/v1/context/training?from=…&to=…", null, null, null),
                new ResourceDescriptor("analytics",
                        "Rendimiento: /analytics/summary (totales, series, carga), "
                                + "/analytics/compare (comparativa de carga entre rangos + señales ACWR/monotony/ramp) "
                                + "y /analytics/correlations (correlaciones transversales semana a semana entre "
                                + "entreno, nutrición, actividad y peso)",
                        null, "/api/v1/analytics/summary", null, null, null),
                new ResourceDescriptor("kdl",
                        "Kore Data Language: import/export de recursos sueltos o en lote "
                                + "({koreType, schemaVersion, payload}); tipos: workout, plan, planned, "
                                + "block, exercise, ingredient, recipe, routine, dietPlan, weightEntry, "
                                + "sessionTemplate, dailyActivity. "
                                + "Import por sección: POST /api/v1/kdl/import/{type} acepta un payload, "
                                + "un array o un documento KDL (un plan completo va como un solo item)",
                        "GET /api/v1/kdl/export", "GET /api/v1/kdl/export?types=…",
                        "POST /api/v1/kdl/import", null, null));

        return new AgentManifestDTO(
                SessionExportDTO.CURRENT_SCHEMA_VERSION,
                "/api/v1",
                CurrentSession.HEADER,
                "API personal de entrenamiento y nutrición. Todos los recursos se leen con GET y se "
                        + "crean con POST (el POST de cada recurso sirve también para importar contenido "
                        + "generado por un agente). Envía el header de sesión en cada petición.",
                resources,
                new Io("GET /api/v1/session/export", "POST /api/v1/session/import"),
                new Docs("/q/openapi", "/q/swagger-ui", "GET /api/v1/agent/context"));
    }

    public AgentContextDTO context(CollectorUser user) {
        return new AgentContextDTO(sessions.export(user), analytics.dashboard(user.id));
    }

    private static ResourceDescriptor crud(String name, String description, String base) {
        return new ResourceDescriptor(
                name, description,
                "GET " + base,
                "GET " + base + "/{id}",
                "POST " + base,
                "PUT " + base + "/{id}",
                "DELETE " + base + "/{id}");
    }
}
