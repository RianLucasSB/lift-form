package com.rianlucassb.liftform.core.usecases.user.register;

public record RegisterUseCaseInput(
        String username,
        String email,
        String password
) {
}
