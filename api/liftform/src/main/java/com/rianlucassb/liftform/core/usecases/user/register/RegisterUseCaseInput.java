package com.rianlucassb.liftform.core.usecases.user.register;

public record RegisterUseCaseInput(
        String userName,
        String email,
        String password
) {
}
