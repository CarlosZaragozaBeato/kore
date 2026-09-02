package com.zensyra.ccollector.core.resource.session;

import com.zensyra.ccollector.core.dto.auth.CollectorUserDTO;
import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.dto.session.SessionExportDTO;
import com.zensyra.ccollector.core.service.session.SessionService;
import com.zensyra.ccollector.core.session.CurrentSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/session")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SessionResource {

    private final SessionService sessions;
    private final CurrentSession session;

    public SessionResource(SessionService sessions, CurrentSession session) {
        this.sessions = sessions;
        this.session = session;
    }

    /** Exporta la sesión actual como documento portable. Requiere sesión. */
    @GET
    @Path("/export")
    public SessionExportDTO export() {
        return sessions.export(session.require());
    }

    /** Importa una sesión desde un documento. No requiere sesión previa. */
    @POST
    @Path("/import")
    public ResponseDTO<CollectorUserDTO> importSession(SessionExportDTO doc) {
        return ResponseDTO.ok(CollectorUserDTO.from(sessions.importSession(doc)));
    }
}
