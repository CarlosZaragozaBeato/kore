package com.zensyra.ccollector.core.session;

import com.zensyra.ccollector.core.domain.auth.CollectorUser;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.NotAuthorizedException;

/**
 * Usuario resuelto para la petición actual a partir del header de sesión.
 * No es autenticación real: solo identifica de qué sesión local son los datos.
 */
@RequestScoped
public class CurrentSession {

    public static final String HEADER = "X-CCollector-Username";

    private CollectorUser user;

    public void set(CollectorUser user) {
        this.user = user;
    }

    public CollectorUser require() {
        if (user == null) {
            throw new NotAuthorizedException(
                    "Falta la sesión: envía el header " + HEADER, "Session");
        }
        return user;
    }

    public Long requireUserId() {
        return require().id;
    }
}
