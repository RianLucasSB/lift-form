package com.rianlucassb.liftform.infraestructure.config.security;

import com.rianlucassb.liftform.core.gateway.security.Hasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class HasherImpl implements Hasher {

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
