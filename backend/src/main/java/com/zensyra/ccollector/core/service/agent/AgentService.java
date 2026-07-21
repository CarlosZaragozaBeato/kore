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
                crud("plans", "Planes de entrenamiento con sesiones planificadas y adherencia", "/api/v1/plans"),
                crud("exercises", "Catálogo de ejercicios de gimnasio", "/api/v1/exercises"),
                crud("routines", "Rutinas de fuerza con ejercicios (series/reps/descanso)", "/api/v1/routines"),
                crud("strengthSessions", "Registro de sesiones de fuerza realizadas", "/api/v1/strength-sessions"),
                crud("recipes", "Recetas con ingredientes, pasos y macros", "/api/v1/recipes"),
                crud("dietPlans", "Planes de dieta con objetivos y comidas por día", "/api/v1/diet-plans"),
                new ResourceDescriptor("suuntoSettings",
                        "Configuración de la integración Suunto (los secretos no se devuelven)",
                        null, "/api/v1/suunto/settings", null, "PUT /api/v1/suunto/settings", null),
                new ResourceDescriptor("analytics",
                        "Resumen de rendimiento (totales, series semanal/mensual, carga)",
                        null, "/api/v1/analytics/summary", null, null, null));

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
