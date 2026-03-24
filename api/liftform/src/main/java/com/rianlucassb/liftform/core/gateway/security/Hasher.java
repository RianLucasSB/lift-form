package com.rianlucassb.liftform.core.gateway.security;

public interface Hasher {
    String hash(String raw);
    boolean verify(String raw, String hashed);
}
