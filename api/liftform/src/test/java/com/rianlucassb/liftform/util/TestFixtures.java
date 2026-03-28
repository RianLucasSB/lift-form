package com.rianlucassb.liftform.util;

import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.usecases.user.register.RegisterUseCaseInput;

import java.time.Instant;
import java.util.UUID;

public final class TestFixtures {

    private TestFixtures() {}

    public static User createUser() {
        return new User(
            UUID.randomUUID(),
            "testuser",
            "test@gmail.com",
            "hashedpassword",
             Instant.now()
        );
    }

    public static User createUser(String email, String username) {
        return new User(
            UUID.randomUUID(),
            username,
            email,
            "hashedpassword",
            Instant.now()

        );
    }

    public static RegisterUseCaseInput createRegisterInput() {
        return new RegisterUseCaseInput("testuser", "test@gmail.com", "testpassword");
    }
}