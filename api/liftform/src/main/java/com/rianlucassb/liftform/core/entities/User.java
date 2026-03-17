package com.rianlucassb.liftform.core.entities;

import java.time.Instant;
import java.util.UUID;

public record User (
        UUID id,
        String userName,
        String email,
        String password,
        Instant createdAt
){
}
