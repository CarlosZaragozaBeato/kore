package com.zensyra.ccollector.core.resource.analytics;

import com.zensyra.ccollector.core.dto.analytics.DashboardDTO;
import com.zensyra.ccollector.core.service.analytics.AnalyticsService;
import com.zensyra.ccollector.core.session.CurrentSession;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/analytics")
@Produces(MediaType.APPLICATION_JSON)
public class AnalyticsResource {

    private final AnalyticsService analytics;
    private final CurrentSession session;

    public AnalyticsResource(AnalyticsService analytics, CurrentSession session) {
        this.analytics = analytics;
        this.session = session;
    }

    /**
     * Resumen de rendimiento. Se devuelve como documento crudo (no envuelto en
     * ResponseDTO) para poder descargarlo/pasarlo tal cual, igual que el export.
     */
    @GET
    @Path("/summary")
    public DashboardDTO summary() {
        return analytics.dashboard(session.requireUserId());
    }
}
