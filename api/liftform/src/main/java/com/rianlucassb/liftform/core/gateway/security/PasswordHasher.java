package com.rianlucassb.liftform.core.gateway.security;

public interface PasswordHasher {
    String hash(String raw);
    boolean verify(String raw, String hashed);
}
