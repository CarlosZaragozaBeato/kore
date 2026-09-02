package com.zensyra.ccollector.core.session;

import com.zensyra.ccollector.core.repository.auth.UserRepository;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Resuelve el usuario de la sesión desde el header en cada petición y lo deja
 * en {@link CurrentSession}. No rechaza peticiones sin header: cada endpoint
 * decide si la sesión es obligatoria (vía {@code require()}).
 */
@Provider
public class SessionFilter implements ContainerRequestFilter {

    private final UserRepository users;
    private final CurrentSession currentSession;

    public SessionFilter(UserRepository users, CurrentSession currentSession) {
        this.users = users;
        this.currentSession = currentSession;
    }

    @Override
    public void filter(ContainerRequestContext ctx) {
        String username = ctx.getHeaderString(CurrentSession.HEADER);
        if (username != null && !username.isBlank()) {
            users.findByUsername(username.trim()).ifPresent(currentSession::set);
        }
    }
}
