package com.rianlucassb.liftform.core.usecases.refreshtoken;

import com.rianlucassb.liftform.core.domain.model.RefreshToken;
import com.rianlucassb.liftform.core.gateway.security.RefreshTokenRepository;

import java.util.Optional;

public class SaveRefreshTokenUseCase {
    private final RefreshTokenRepository repository;

    public SaveRefreshTokenUseCase(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    public void execute(RefreshToken token) {
        Optional<RefreshToken> existingTokenOptional = repository.findByUserId(token.userId());

        if(existingTokenOptional.isPresent()){
            RefreshToken existingToken = existingTokenOptional.get();
            if(existingToken.expiresAt().isAfter(token.createdAt()) && !existingToken.revoked()){
                throw new RuntimeException("Usuário já possui um refresh token válido"); // Todo: criar exception personalizada
            }
            RefreshToken revokedToken = new RefreshToken(
                    existingToken.tokenHash(),
                    existingToken.userId(),
                    existingToken.createdAt(),
                    existingToken.expiresAt(),
                    true
            );
            repository.save(revokedToken);
        }

        repository.save(token);
    }
}
