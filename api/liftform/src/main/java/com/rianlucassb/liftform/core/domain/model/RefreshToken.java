package com.rianlucassb.liftform.core.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
        String tokenHash,
        UUID userId,
        Instant createdAt,
        Instant expiresAt,
        Boolean revoked
) {

    public RefreshToken revoke() {
        return new RefreshToken(this.tokenHash, this.userId, this.createdAt, this.expiresAt, true);
    }
}
