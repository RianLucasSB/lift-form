package com.rianlucassb.liftform.core.domain.model;

import java.time.Instant;

public record RefreshToken(
        Long id,
        String hashedToken,
        User user,
        Instant expiresAt,
        Instant createdAt,
        Boolean revoked
) {
}
