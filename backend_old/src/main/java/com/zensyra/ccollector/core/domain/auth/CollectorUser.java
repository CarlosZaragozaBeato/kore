package com.zensyra.ccollector.core.domain.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Usuario = sesión local. No hay contraseña: la app no se expone, la sesión se
 * identifica solo por username y se transporta vía export/import.
 */
@Entity
@Table(name = "users")
public class CollectorUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String username;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
