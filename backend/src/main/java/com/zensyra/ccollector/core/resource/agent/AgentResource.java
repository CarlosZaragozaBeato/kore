package com.zensyra.ccollector.core.resource.agent;

import com.zensyra.ccollector.core.dto.agent.AgentContextDTO;
import com.zensyra.ccollector.core.dto.agent.AgentManifestDTO;
import com.zensyra.ccollector.core.service.agent.AgentService;
import com.zensyra.ccollector.core.session.CurrentSession;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Endpoints de descubrimiento para agentes. El manifest es estático (no
 * requiere sesión); el context devuelve los datos del usuario (sí la requiere).
 */
@Path("/agent")
@Produces(MediaType.APPLICATION_JSON)
public class AgentResource {

    private final AgentService agent;
    private final CurrentSession session;

    public AgentResource(AgentService agent, CurrentSession session) {
        this.agent = agent;
        this.session = session;
    }

    /** Mapa de recursos y cómo leerlos/escribirlos. No requiere sesión. */
    @GET
    @Path("/manifest")
    public AgentManifestDTO manifest() {
        return agent.manifest();
    }

    /** Todo el contexto del usuario (sesión completa + analítica). */
    @GET
    @Path("/context")
    public AgentContextDTO context() {
        return agent.context(session.require());
    }
}
