package com.zensyra.ccollector.core.service.suunto;

import com.zensyra.ccollector.core.crypto.CryptoService;
import com.zensyra.ccollector.core.domain.suunto.SuuntoSettings;
import com.zensyra.ccollector.core.dto.suunto.SuuntoSettingsRequest;
import com.zensyra.ccollector.core.repository.suunto.SuuntoSettingsRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class SuuntoSettingsService {

    private final SuuntoSettingsRepository repository;
    private final CryptoService crypto;

    public SuuntoSettingsService(SuuntoSettingsRepository repository, CryptoService crypto) {
        this.repository = repository;
        this.crypto = crypto;
    }

    public Optional<SuuntoSettings> find(Long userId) {
        return repository.findByUser(userId);
    }

    @Transactional
    public SuuntoSettings save(Long userId, SuuntoSettingsRequest req) {
        Optional<SuuntoSettings> existing = repository.findByUser(userId);
        SuuntoSettings s = existing.orElseGet(() -> {
            SuuntoSettings created = new SuuntoSettings();
            created.userId = userId;
            return created;
        });

        if (req.enabled() != null) {
            s.enabled = req.enabled();
        }
        if (req.clientId() != null) {
            s.clientId = blankToNull(req.clientId());
        }
        // Los secretos solo se actualizan si llegan con valor.
        if (isPresent(req.clientSecret())) {
            s.clientSecretEnc = crypto.encrypt(req.clientSecret().trim());
        }
        if (isPresent(req.refreshToken())) {
            s.refreshTokenEnc = crypto.encrypt(req.refreshToken().trim());
        }
        if (isPresent(req.subscriptionKey())) {
            s.subscriptionKeyEnc = crypto.encrypt(req.subscriptionKey().trim());
        }
        s.updatedAt = Instant.now();
        if (existing.isEmpty()) {
            repository.persist(s);
        }
        return s;
    }

    /** Credenciales en claro para la sincronización. */
    public DecryptedCredentials decrypt(SuuntoSettings s) {
        return new DecryptedCredentials(
                s.clientId,
                crypto.decrypt(s.clientSecretEnc),
                crypto.decrypt(s.refreshTokenEnc),
                crypto.decrypt(s.subscriptionKeyEnc));
    }

    @Transactional
    public void updateRefreshToken(Long userId, String newRefreshToken) {
        repository.findByUser(userId).ifPresent(s -> s.refreshTokenEnc = crypto.encrypt(newRefreshToken));
    }

    @Transactional
    public void markSynced(Long userId, Instant when) {
        repository.findByUser(userId).ifPresent(s -> s.lastSyncAt = when);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record DecryptedCredentials(
            String clientId, String clientSecret, String refreshToken, String subscriptionKey) {

        public boolean complete() {
            return isSet(clientId) && isSet(clientSecret) && isSet(refreshToken) && isSet(subscriptionKey);
        }

        private static boolean isSet(String v) {
            return v != null && !v.isBlank();
        }
    }
}
