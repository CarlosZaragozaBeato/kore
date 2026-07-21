package com.zensyra.ccollector.core.domain.suunto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Configuración de la integración Suunto de un usuario. Las credenciales se
 * guardan cifradas (columnas {@code *_enc}); nunca se exponen en claro por la API.
 */
@Entity
@Table(name = "suunto_settings")
public class SuuntoSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    public Long userId;

    @Column(nullable = false)
    public boolean enabled;

    @Column(name = "client_id")
    public String clientId;

    @Column(name = "client_secret_enc")
    public String clientSecretEnc;

    @Column(name = "refresh_token_enc")
    public String refreshTokenEnc;

    @Column(name = "subscription_key_enc")
    public String subscriptionKeyEnc;

    @Column(name = "last_sync_at")
    public Instant lastSyncAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
