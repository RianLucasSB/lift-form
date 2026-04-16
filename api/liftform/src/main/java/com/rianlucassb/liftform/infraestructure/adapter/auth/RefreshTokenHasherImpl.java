package com.rianlucassb.liftform.infraestructure.adapter.auth;

import com.rianlucassb.liftform.core.gateway.security.RefreshTokenHasher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class RefreshTokenHasherImpl implements RefreshTokenHasher {
    @Override
    public String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Override
    public boolean verify(String raw, String hashed) {
        return hash(raw).equals(hashed);
    }
}
