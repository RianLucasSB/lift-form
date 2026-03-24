package com.rianlucassb.liftform.core.gateway.user;

import com.rianlucassb.liftform.core.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    User save(User user);
}