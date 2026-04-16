package com.rianlucassb.liftform.infraestructure.adapter.auth;

import com.rianlucassb.liftform.core.gateway.security.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasherImpl implements PasswordHasher {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String hash(String raw) {
        return bcrypt.encode(raw);
    }

    @Override
    public boolean verify(String raw, String hashed) {
        return bcrypt.matches(raw, hashed);
    }
}
