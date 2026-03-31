package com.rianlucassb.liftform.core.usecases.user.refreshtoken;

public record RefreshTokenUseCaseOutput (
        String accessToken,
        String refreshToken
){
}
