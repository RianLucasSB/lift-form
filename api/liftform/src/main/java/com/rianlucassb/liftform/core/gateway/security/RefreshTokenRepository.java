package com.rianlucassb.liftform.core.gateway.security;

import com.rianlucassb.liftform.core.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByHashedToken(String hashedToken);
    void save(RefreshToken token);
    Optional<RefreshToken> findByUserId(UUID userId);
    Optional<RefreshToken> findActiveByUserId(UUID userId);
}
