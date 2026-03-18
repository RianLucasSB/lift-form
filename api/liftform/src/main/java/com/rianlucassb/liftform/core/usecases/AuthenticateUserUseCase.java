package com.rianlucassb.liftform.core.usecases;

import com.rianlucassb.liftform.core.domain.exception.InvalidCredentialsException;
import com.rianlucassb.liftform.core.gateway.security.PasswordHasher;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.core.domain.model.User;

public class AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthenticateUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public User execute(String login, String password) {
        User user = userRepository.findByEmail(login)
                .or(() -> userRepository.findByUsername(login))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordMatches(password, user.password())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        return user;
    }

    private boolean passwordMatches(String raw, String hashed) {
        return passwordHasher.verify(raw, hashed);
    }
}