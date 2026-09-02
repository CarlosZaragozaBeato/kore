package com.zensyra.ccollector.core.resource.suunto;

import com.zensyra.ccollector.core.domain.auth.CollectorUser;
import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.dto.suunto.PurgeResultDTO;
import com.zensyra.ccollector.core.dto.suunto.SuuntoSettingsDTO;
import com.zensyra.ccollector.core.dto.suunto.SuuntoSettingsRequest;
import com.zensyra.ccollector.core.dto.suunto.SyncResultDTO;
import com.zensyra.ccollector.core.service.suunto.SuuntoRetentionService;
import com.zensyra.ccollector.core.service.suunto.SuuntoSettingsService;
import com.zensyra.ccollector.core.service.suunto.SuuntoSyncService;
import com.zensyra.ccollector.core.session.CurrentSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/suunto")
@Produces(MediaType.APPLICATION_JSON)
public class SuuntoResource {

    private final SuuntoSettingsService settingsService;
    private final SuuntoSyncService syncService;
    private final SuuntoRetentionService retentionService;
    private final CurrentSession session;

    public SuuntoResource(SuuntoSettingsService settingsService,
                          SuuntoSyncService syncService,
                          SuuntoRetentionService retentionService,
                          CurrentSession session) {
        this.settingsService = settingsService;
        this.syncService = syncService;
        this.retentionService = retentionService;
        this.session = session;
    }

    @GET
    @Path("/settings")
    public ResponseDTO<SuuntoSettingsDTO> getSettings() {
        Long userId = session.requireUserId();
        return ResponseDTO.ok(SuuntoSettingsDTO.from(settingsService.find(userId).orElse(null)));
    }

    @PUT
    @Path("/settings")
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<SuuntoSettingsDTO> saveSettings(SuuntoSettingsRequest request) {
        Long userId = session.requireUserId();
        return ResponseDTO.ok(SuuntoSettingsDTO.from(settingsService.save(userId, request)));
    }

    @POST
    @Path("/sync")
    public ResponseDTO<SyncResultDTO> sync() {
        CollectorUser user = session.require();
        return ResponseDTO.ok(syncService.sync(user.id));
    }

    /**
     * Aplica la retención mínima: borra los entrenos de Suunto anteriores a la
     * ventana configurada (por defecto, mes actual + anterior). Es recuperable
     * con un re-sync, así que evita crecer sin límite.
     */
    @POST
    @Path("/purge")
    public ResponseDTO<PurgeResultDTO> purge() {
        Long userId = session.requireUserId();
        return ResponseDTO.ok(retentionService.purge(userId));
    }
}
