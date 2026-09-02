package com.zensyra.ccollector.core.repository.auth;

import com.zensyra.ccollector.core.domain.auth.CollectorUser;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class UserRepository implements PanacheRepository<CollectorUser> {

    public Optional<CollectorUser> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }
}
