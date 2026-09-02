package com.zensyra.ccollector.core.service.suunto;

import com.zensyra.ccollector.core.client.suunto.SuuntoApiClient;
import com.zensyra.ccollector.core.client.suunto.SuuntoOAuthClient;
import com.zensyra.ccollector.core.client.suunto.SuuntoTokenResponse;
import com.zensyra.ccollector.core.client.suunto.SuuntoWorkout;
import com.zensyra.ccollector.core.domain.suunto.SuuntoSettings;
import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.domain.workout.WorkoutSource;
import com.zensyra.ccollector.core.dto.suunto.SyncResultDTO;
import com.zensyra.ccollector.core.repository.workout.WorkoutRepository;
import com.zensyra.ccollector.core.service.suunto.SuuntoSettingsService.DecryptedCredentials;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

@ApplicationScoped
public class SuuntoSyncService {

    private static final Logger LOG = Logger.getLogger(SuuntoSyncService.class);

    private final SuuntoSettingsService settingsService;
    private final WorkoutRepository workouts;
    private final SuuntoOAuthClient oauthClient;
    private final SuuntoApiClient apiClient;

    public SuuntoSyncService(SuuntoSettingsService settingsService,
                             WorkoutRepository workouts,
                             @RestClient SuuntoOAuthClient oauthClient,
                             @RestClient SuuntoApiClient apiClient) {
        this.settingsService = settingsService;
        this.workouts = workouts;
        this.oauthClient = oauthClient;
        this.apiClient = apiClient;
    }

    @Transactional
    public SyncResultDTO sync(Long userId) {
        SuuntoSettings settings = settingsService.find(userId)
                .orElseThrow(() -> new BadRequestException("La integración con Suunto no está configurada"));
        if (!settings.enabled) {
            throw new BadRequestException("La integración con Suunto está deshabilitada");
        }
        DecryptedCredentials creds = settingsService.decrypt(settings);
        if (!creds.complete()) {
            throw new BadRequestException("Faltan credenciales de Suunto (client id/secret, refresh token o subscription key)");
        }

        SuuntoTokenResponse token = refreshToken(userId, creds);
        var suuntoWorkouts = fetchWorkouts(token, creds);

        int imported = 0;
        int updated = 0;
        int skipped = 0;
        for (SuuntoWorkout sw : suuntoWorkouts) {
            if (sw.workoutKey() == null) {
                skipped++;
                continue;
            }
            var existing = workouts.findBySourceId(userId, WorkoutSource.SUUNTO, sw.workoutKey());
            if (existing.isPresent()) {
                // Re-sync: rellena/corrige el entreno ya importado en vez de
                // ignorarlo (FC, tipo, kcal y pasos que faltaban).
                SuuntoWorkoutMapper.applyMetrics(existing.get(), sw);
                updated++;
            } else {
                Workout w = SuuntoWorkoutMapper.toWorkout(sw, userId);
                workouts.persist(w);
                imported++;
            }
        }

        Instant now = Instant.now();
        settingsService.markSynced(userId, now);
        return new SyncResultDTO(imported, updated, skipped, suuntoWorkouts.size(), now);
    }

    private SuuntoTokenResponse refreshToken(Long userId, DecryptedCredentials creds) {
        String basic = "Basic " + Base64.getEncoder().encodeToString(
                (creds.clientId() + ":" + creds.clientSecret()).getBytes(StandardCharsets.UTF_8));
        SuuntoTokenResponse token;
        try {
            token = oauthClient.refresh(basic, "refresh_token", creds.refreshToken());
        } catch (WebApplicationException | ProcessingException e) {
            LOG.warn("Fallo refrescando el token de Suunto", e);
            throw suuntoError("No se pudo refrescar el token de Suunto (revisa client id/secret y refresh token)");
        }
        if (token == null || token.accessToken() == null) {
            throw suuntoError("Suunto no devolvió un access token");
        }
        // Suunto puede rotar el refresh token; guardamos el nuevo si cambia.
        if (token.refreshToken() != null && !Objects.equals(token.refreshToken(), creds.refreshToken())) {
            settingsService.updateRefreshToken(userId, token.refreshToken());
        }
        return token;
    }

    private java.util.List<SuuntoWorkout> fetchWorkouts(SuuntoTokenResponse token, DecryptedCredentials creds) {
        try {
            var response = apiClient.listWorkouts("Bearer " + token.accessToken(), creds.subscriptionKey());
            return response == null ? java.util.List.of() : response.workoutsOrEmpty();
        } catch (WebApplicationException | ProcessingException e) {
            LOG.warn("Fallo obteniendo workouts de Suunto", e);
            throw suuntoError("No se pudieron obtener los entrenos de Suunto (revisa la subscription key)");
        }
    }

    private WebApplicationException suuntoError(String message) {
        return new WebApplicationException(message, Response.Status.BAD_GATEWAY);
    }
}
