package com.zensyra.ccollector.core.exception.auth;

import jakarta.ws.rs.BadRequestException;

/** El login exige un username no vacío. */
public class NoUsernameException extends BadRequestException {

    public NoUsernameException() {
        super("El username es obligatorio");
    }
}
