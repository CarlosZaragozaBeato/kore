package com.zensyra.ccollector.core.service.auth;

import com.zensyra.ccollector.core.domain.auth.CollectorUser;
import com.zensyra.ccollector.core.exception.auth.NoUsernameException;
import com.zensyra.ccollector.core.repository.auth.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;

@ApplicationScoped
public class AuthService {

    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    /**
     * Login-or-create: si el username existe devuelve esa sesión, si no la crea.
     * No hay contraseña por diseño.
     */
    @Transactional
    public CollectorUser login(String username) {
        String normalized = username == null ? "" : username.trim();
        if (normalized.isEmpty()) {
            throw new NoUsernameException();
        }
        return users.findByUsername(normalized).orElseGet(() -> {
            CollectorUser user = new CollectorUser();
            user.username = normalized;
            user.createdAt = Instant.now();
            users.persist(user);
            return user;
        });
    }

    public CollectorUser requireByUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (normalized.isEmpty()) {
            throw new NoUsernameException();
        }
        return users.findByUsername(normalized).orElseThrow(NoUsernameException::new);
    }
}
