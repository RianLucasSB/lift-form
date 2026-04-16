package com.rianlucassb.liftform.infraestructure.adapter.transaction.auth;

import com.rianlucassb.liftform.core.usecases.user.register.RegisterUseCase;
import com.rianlucassb.liftform.core.usecases.user.register.RegisterUseCaseInput;
import com.rianlucassb.liftform.core.usecases.user.register.RegisterUseCaseOutput;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

@Primary
public class TransactionalRegisterUseCase implements RegisterUseCase {

    private final RegisterUseCase registerUseCase;

    public TransactionalRegisterUseCase(RegisterUseCase registerUseCase) {
        this.registerUseCase = registerUseCase;
    }

    @Override
    @Transactional
    public RegisterUseCaseOutput execute(RegisterUseCaseInput input) {
        return registerUseCase.execute(input);
    }
}
