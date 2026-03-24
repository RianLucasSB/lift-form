package com.rianlucassb.liftform.core.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
        String hashedToken,
        UUID userId,
        Instant createdAt,
        Instant expiresAt,
        Boolean revoked
) {
}
