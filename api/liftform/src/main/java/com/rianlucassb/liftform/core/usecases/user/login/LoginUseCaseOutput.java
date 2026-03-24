package com.rianlucassb.liftform.core.usecases.user.login;

public record LoginUseCaseOutput(
        String accessToken,
        String refreshToken
) {
}
