package com.zensyra.ccollector.core.repository.suunto;

import com.zensyra.ccollector.core.domain.suunto.SuuntoSettings;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class SuuntoSettingsRepository implements PanacheRepository<SuuntoSettings> {

    public Optional<SuuntoSettings> findByUser(Long userId) {
        return find("userId", userId).firstResultOptional();
    }
}
