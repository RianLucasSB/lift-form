package com.rianlucassb.liftform.infraestructure.transaction;

import com.rianlucassb.liftform.core.usecases.user.login.LoginUseCase;
import com.rianlucassb.liftform.core.usecases.user.login.LoginUseCaseInput;
import com.rianlucassb.liftform.core.usecases.user.login.LoginUseCaseOutput;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

@Primary
public class TransactionalLoginUseCase implements LoginUseCase {

    private final LoginUseCase loginUseCase;

    public TransactionalLoginUseCase(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @Override
    @Transactional
    public LoginUseCaseOutput execute(LoginUseCaseInput input) {
        return loginUseCase.execute(input);
    }
}