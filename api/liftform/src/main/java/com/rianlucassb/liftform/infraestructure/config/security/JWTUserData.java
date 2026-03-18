package com.rianlucassb.liftform.infraestructure.config.security;

import lombok.Builder;

import java.util.UUID;

@Builder
public record JWTUserData(
        String id,
        String email
) {
}
