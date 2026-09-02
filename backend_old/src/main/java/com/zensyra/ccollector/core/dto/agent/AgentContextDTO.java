package com.zensyra.ccollector.core.dto.agent;

import com.zensyra.ccollector.core.dto.analytics.DashboardDTO;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO;

/**
 * Todo el contexto del usuario en una llamada: la sesión completa (mismo
 * formato que el export) más el resumen analítico. Un agente puede leer esto
 * y razonar/planificar sobre ello.
 */
public record AgentContextDTO(SessionExportDTO session, DashboardDTO analytics) {
}
