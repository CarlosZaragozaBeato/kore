package com.zensyra.ccollector.core.dto.suunto;

/**
 * Actualización de la configuración Suunto. Los secretos son opcionales: si
 * llegan vacíos/nulos se conserva el valor ya guardado (no hay que reescribirlos
 * en cada edición).
 */
public record SuuntoSettingsRequest(
        Boolean enabled,
        String clientId,
        String clientSecret,
        String refreshToken,
        String subscriptionKey
) {
}
