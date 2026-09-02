package com.zensyra.ccollector.core.service.plan;

import com.zensyra.ccollector.core.domain.plan.LoadStance;
import com.zensyra.ccollector.core.domain.plan.PlannedSession;
import com.zensyra.ccollector.core.domain.plan.PlannedStep;
import com.zensyra.ccollector.core.domain.plan.SessionStatus;
import com.zensyra.ccollector.core.domain.plan.StepKind;
import com.zensyra.ccollector.core.dto.analytics.LoadSignalsDTO;
import com.zensyra.ccollector.core.dto.plan.PlanRecommendationDTO;
import com.zensyra.ccollector.core.dto.plan.PlanRequest;
import com.zensyra.ccollector.core.dto.plan.PlannedSessionDTO;
import com.zensyra.ccollector.core.dto.plan.PlannedSessionRequest;
import com.zensyra.ccollector.core.dto.plan.VariantSuggestionDTO;
import com.zensyra.ccollector.core.repository.plan.PlannedSessionRepository;
import com.zensyra.ccollector.core.repository.plan.PlannedStepRepository;
import com.zensyra.ccollector.core.service.analytics.LoadComparisonService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Inteligencia de planificación (Fase E): recomienda una postura de carga a
 * partir de las señales de entrenamiento (ACWR/monotonía/rampa) y genera
 * variantes de descarga/subida de una sesión para que elijas según la situación.
 * Los ajustes son deterministas y conservadores; la intensidad (ritmo/FC) se
 * mantiene y sólo cambia el volumen (distancia, duración, nº de repeticiones).
 */
@ApplicationScoped
public class PlanIntelligenceService {

    /** Factor de volumen para una descarga (-30 %) y una subida (+10 %). */
    private static final double DELOAD_FACTOR = 0.7;
    private static final double BUILD_FACTOR = 1.1;

    private final PlannedSessionRepository sessions;
    private final PlannedStepRepository steps;
    private final PlannedSessionService plannedSessions;
    private final LoadComparisonService load;

    public PlanIntelligenceService(PlannedSessionRepository sessions, PlannedStepRepository steps,
                                   PlannedSessionService plannedSessions, LoadComparisonService load) {
        this.sessions = sessions;
        this.steps = steps;
        this.plannedSessions = plannedSessions;
        this.load = load;
    }

    /** Recomienda descarga/mantener/subir según las señales de carga actuales. */
    public PlanRecommendationDTO recommend(Long userId) {
        LoadSignalsDTO s = load.signals(userId);
        boolean risk = "RISK".equals(s.acwrState())
                || "RISK".equals(s.rampState())
                || "RISK".equals(s.monotonyState());
        if (risk) {
            return rec(LoadStance.DELOAD, deloadReason(s), s);
        }
        if (s.acwr() == null) {
            return rec(LoadStance.MAINTAIN,
                    "Aún no hay datos de carga suficientes; mantén la carga planificada.", s);
        }
        if (s.acwr() < 0.8 && !"WARN".equals(s.rampState())) {
            return rec(LoadStance.BUILD,
                    "ACWR " + s.acwr() + " por debajo de 0.8: hay margen para subir la carga.", s);
        }
        return rec(LoadStance.MAINTAIN,
                "Carga en rango (ACWR " + s.acwr() + "); mantén el plan.", s);
    }

    /**
     * Genera variantes de descarga y subida de una sesión suelta del calendario,
     * como hermanas propuestas de su mismo grupo, y devuelve el grupo completo
     * junto con la recomendación de qué postura seguir.
     */
    @Transactional
    public VariantSuggestionDTO generateVariants(Long userId, Long sessionId) {
        PlannedSession base = sessions.findByIdAndUser(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Sesión planificada no encontrada"));
        if (base.planId != null) {
            throw new BadRequestException(
                    "Genera variantes sobre sesiones sueltas del calendario, no sobre las de un plan");
        }
        // La base ancla el grupo y queda como "Carga normal".
        if (base.variantGroup == null) {
            base.variantGroup = UUID.randomUUID().toString();
        }
        if (base.variantLabel == null || base.variantLabel.isBlank()) {
            base.variantLabel = "Carga normal";
        }
        String group = base.variantGroup;
        List<PlannedStep> baseSteps = steps.listBySession(base.id);

        plannedSessions.create(userId, scaled(base, baseSteps, group, DELOAD_FACTOR, -1, "Descarga"));
        plannedSessions.create(userId, scaled(base, baseSteps, group, BUILD_FACTOR, +1, "Subida de carga"));

        List<PlannedSessionDTO> variants = plannedSessions.list(userId).stream()
                .filter(d -> group.equals(d.variantGroup()))
                .toList();
        return new VariantSuggestionDTO(base.id, group, recommend(userId), variants);
    }

    /** Construye una petición de sesión escalando el volumen de la base. */
    private PlannedSessionRequest scaled(PlannedSession base, List<PlannedStep> baseSteps,
                                         String group, double factor, int repeatDelta, String label) {
        Double distance = scaleDouble(base.targetDistanceMeters, factor);
        Long duration = scaleLong(base.targetDurationSeconds, factor);

        List<PlanRequest.StepRequest> stepReqs = new ArrayList<>();
        for (PlannedStep st : baseSteps) {
            boolean interval = st.kind == StepKind.INTERVAL;
            // En series, el volumen cambia por nº de repeticiones; en lo continuo, por distancia/tiempo.
            int repeat = interval ? Math.max(1, st.repeat + repeatDelta) : st.repeat;
            Double sd = interval ? st.targetDistanceMeters : scaleDouble(st.targetDistanceMeters, factor);
            Long sdur = interval ? st.targetDurationSeconds : scaleLong(st.targetDurationSeconds, factor);
            stepReqs.add(new PlanRequest.StepRequest(
                    st.kind, repeat, sd, sdur,
                    st.targetPaceMinSecPerKm, st.targetPaceMaxSecPerKm,
                    st.targetHrMin, st.targetHrMax, st.recoverySeconds, st.note));
        }

        return new PlannedSessionRequest(base.date, base.type, distance, duration, base.description,
                SessionStatus.PROPOSED, group, label, stepReqs);
    }

    private PlanRecommendationDTO rec(LoadStance stance, String reason, LoadSignalsDTO signals) {
        return new PlanRecommendationDTO(stance, label(stance), reason, signals);
    }

    private String label(LoadStance stance) {
        return switch (stance) {
            case DELOAD -> "Descarga";
            case BUILD -> "Subir carga";
            case MAINTAIN -> "Mantener";
        };
    }

    private String deloadReason(LoadSignalsDTO s) {
        List<String> factors = new ArrayList<>();
        if ("RISK".equals(s.acwrState())) {
            factors.add("ACWR " + s.acwr() + " (riesgo)");
        }
        if ("RISK".equals(s.rampState())) {
            factors.add("rampa +" + s.rampPct() + "%");
        }
        if ("RISK".equals(s.monotonyState())) {
            factors.add("monotonía " + (s.monotony() == null ? "alta" : s.monotony()));
        }
        return String.join(" y ", factors) + " sugieren bajar la carga.";
    }

    /** Escala un valor boxed conservando el null (evita el desempaquetado del ternario). */
    private static Double scaleDouble(Double v, double factor) {
        return v == null ? null : (double) Math.round(v * factor);
    }

    private static Long scaleLong(Long v, double factor) {
        return v == null ? null : Math.round(v * factor);
    }
}
