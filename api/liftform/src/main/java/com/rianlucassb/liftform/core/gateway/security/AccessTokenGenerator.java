package com.rianlucassb.liftform.core.gateway.security;

import com.rianlucassb.liftform.core.domain.model.User;

public interface AccessTokenGenerator {
    String generate(User user);
}