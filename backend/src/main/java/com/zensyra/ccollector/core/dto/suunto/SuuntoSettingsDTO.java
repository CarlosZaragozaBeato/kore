package com.zensyra.ccollector.core.dto.suunto;

import com.zensyra.ccollector.core.domain.suunto.SuuntoSettings;

import java.time.Instant;

/**
 * Vista de la configuración Suunto para el cliente. No expone secretos: solo
 * indica qué credenciales están guardadas (booleanos {@code has*}).
 */
public record SuuntoSettingsDTO(
        boolean enabled,
        String clientId,
        boolean hasClientSecret,
        boolean hasRefreshToken,
        boolean hasSubscriptionKey,
        Instant lastSyncAt
) {

    public static SuuntoSettingsDTO from(SuuntoSettings s) {
        if (s == null) {
            return new SuuntoSettingsDTO(false, null, false, false, false, null);
        }
        return new SuuntoSettingsDTO(
                s.enabled,
                s.clientId,
                s.clientSecretEnc != null,
                s.refreshTokenEnc != null,
                s.subscriptionKeyEnc != null,
                s.lastSyncAt);
    }
}
