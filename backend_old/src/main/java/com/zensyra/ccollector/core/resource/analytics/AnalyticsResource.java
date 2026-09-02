package com.zensyra.ccollector.core.resource.analytics;

import com.zensyra.ccollector.core.dto.analytics.CorrelationReportDTO;
import com.zensyra.ccollector.core.dto.analytics.DashboardDTO;
import com.zensyra.ccollector.core.dto.analytics.LoadComparisonDTO;
import com.zensyra.ccollector.core.service.analytics.AnalyticsService;
import com.zensyra.ccollector.core.service.analytics.CorrelationService;
import com.zensyra.ccollector.core.service.analytics.LoadComparisonService;
import com.zensyra.ccollector.core.session.CurrentSession;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Path("/analytics")
@Produces(MediaType.APPLICATION_JSON)
public class AnalyticsResource {

    private final AnalyticsService analytics;
    private final LoadComparisonService comparison;
    private final CorrelationService correlations;
    private final CurrentSession session;

    public AnalyticsResource(AnalyticsService analytics, LoadComparisonService comparison,
                             CorrelationService correlations, CurrentSession session) {
        this.analytics = analytics;
        this.comparison = comparison;
        this.correlations = correlations;
        this.session = session;
    }

    /**
     * Informe de correlaciones transversales (entreno × nutrición × actividad ×
     * peso), semana a semana. Documento crudo, de solo lectura.
     */
    @GET
    @Path("/correlations")
    public CorrelationReportDTO correlations() {
        return correlations.report(session.requireUserId());
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

    /**
     * Comparativa de carga entre dos rangos. Sin parámetros: semana actual vs
     * previa. Fechas en ISO (yyyy-MM-dd); si se omite el rango previo, se toma el
     * inmediatamente anterior de la misma longitud. Documento crudo.
     */
    @GET
    @Path("/compare")
    public LoadComparisonDTO compare(
            @QueryParam("currentStart") String currentStart,
            @QueryParam("currentEnd") String currentEnd,
            @QueryParam("previousStart") String previousStart,
            @QueryParam("previousEnd") String previousEnd) {
        return comparison.compare(
                session.requireUserId(),
                parse(currentStart), parse(currentEnd),
                parse(previousStart), parse(previousEnd));
    }

    private static LocalDate parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Fecha inválida: '" + value + "' (usa yyyy-MM-dd)");
        }
    }
}
