package com.rianlucassb.liftform.core.gateway.security;

public interface PasswordHasher {
    String hash(String password);
    boolean verify(String password, String hashedPassword);
}
