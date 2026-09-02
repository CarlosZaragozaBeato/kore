package com.zensyra.ccollector.core.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Prefija todos los endpoints REST bajo /api/v1.
 * Los endpoints de infraestructura (health) viven fuera, en /q/health.
 */
@ApplicationPath("/api/v1")
public class RestApplication extends Application {
}
