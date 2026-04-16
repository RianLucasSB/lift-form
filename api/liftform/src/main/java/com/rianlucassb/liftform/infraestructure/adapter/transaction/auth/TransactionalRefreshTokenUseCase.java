package com.rianlucassb.liftform.infraestructure.adapter.transaction.auth;

import com.rianlucassb.liftform.core.usecases.user.refreshtoken.RefreshTokenUseCase;
import com.rianlucassb.liftform.core.usecases.user.refreshtoken.RefreshTokenUseCaseInput;
import com.rianlucassb.liftform.core.usecases.user.refreshtoken.RefreshTokenUseCaseOutput;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

@Primary
public class TransactionalRefreshTokenUseCase implements RefreshTokenUseCase {

    private final RefreshTokenUseCase refreshTokenUseCase;

    public TransactionalRefreshTokenUseCase(RefreshTokenUseCase refreshTokenUseCase) {
        this.refreshTokenUseCase = refreshTokenUseCase;
    }

    @Override
    @Transactional
    public RefreshTokenUseCaseOutput execute(RefreshTokenUseCaseInput input) {
        return refreshTokenUseCase.execute(input);
    }
}