package com.rianlucassb.liftform.infraestructure.adapter.transaction.auth;

import com.rianlucassb.liftform.core.usecases.user.logout.LogoutUseCase;
import com.rianlucassb.liftform.core.usecases.user.logout.LogoutUseCaseInput;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

@Primary
public class TransactionalLogoutUseCase implements LogoutUseCase {

    private final LogoutUseCase logoutUseCase;

    public TransactionalLogoutUseCase(LogoutUseCase logoutUseCase) {
        this.logoutUseCase = logoutUseCase;
    }

    @Override
    @Transactional
    public void execute(LogoutUseCaseInput input) {
        logoutUseCase.execute(input);
    }
}
