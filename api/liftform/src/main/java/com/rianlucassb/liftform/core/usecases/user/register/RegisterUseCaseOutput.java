package com.rianlucassb.liftform.core.usecases.user.register;

public record RegisterUseCaseOutput(
        String accessToken,
        String refreshToken
) {
}
