package com.zensyra.ccollector.core.resource.energy;

import com.zensyra.ccollector.core.dto.energy.EnergySummaryDTO;
import com.zensyra.ccollector.core.service.energy.EnergyService;
import com.zensyra.ccollector.core.session.CurrentSession;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/energy")
@Produces(MediaType.APPLICATION_JSON)
public class EnergyResource {

    private final EnergyService energy;
    private final CurrentSession session;

    public EnergyResource(EnergyService energy, CurrentSession session) {
        this.energy = energy;
        this.session = session;
    }

    /**
     * Balance energético (consumidas vs quemadas), serie diaria y semanal. Crudo
     * (no envuelto en ResponseDTO), como {@code /analytics/summary}.
     */
    @GET
    @Path("/summary")
    public EnergySummaryDTO summary() {
        return energy.summary(session.requireUserId());
    }
}
