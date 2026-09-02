package com.zensyra.ccollector.core.dto.auth;

import com.zensyra.ccollector.core.domain.auth.CollectorUser;

import java.time.Instant;

public record CollectorUserDTO(Long id, String username, Instant createdAt) {

    public static CollectorUserDTO from(CollectorUser user) {
        return new CollectorUserDTO(user.id, user.username, user.createdAt);
    }
}
