package com.zensyra.ccollector.core.resource.context;

import com.zensyra.ccollector.core.dto.context.TrainingContextDTO;
import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.service.context.TrainingContextService;
import com.zensyra.ccollector.core.session.CurrentSession;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Contexto de entrenamiento acotado a un rango, para entregar a un agente.
 * {@code GET /api/v1/context/training?from=YYYY-MM-DD&to=YYYY-MM-DD}.
 */
@Path("/context")
@Produces(MediaType.APPLICATION_JSON)
public class TrainingContextResource {

    private final TrainingContextService context;
    private final CurrentSession session;

    public TrainingContextResource(TrainingContextService context, CurrentSession session) {
        this.context = context;
        this.session = session;
    }

    @GET
    @Path("/training")
    public ResponseDTO<TrainingContextDTO> training(@QueryParam("from") String from, @QueryParam("to") String to) {
        return ResponseDTO.ok(context.build(session.require(), parse(from, "from"), parse(to, "to")));
    }

    private LocalDate parse(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Falta el parámetro '" + field + "' (YYYY-MM-DD)");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("'" + field + "' no es una fecha válida (YYYY-MM-DD): " + value);
        }
    }
}
