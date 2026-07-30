package com.rianlucassb.liftform.core.usecases.user.logout;

import com.rianlucassb.liftform.core.gateway.security.RefreshTokenRepository;

import java.util.UUID;

public class LogoutUseCaseImpl implements LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutUseCaseImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void execute(LogoutUseCaseInput input) {
        UUID userId = UUID.fromString(input.userId());

        refreshTokenRepository.findActiveByUserId(userId)
                .ifPresent(refreshToken -> refreshTokenRepository.save(refreshToken.revoke()));
    }
}
