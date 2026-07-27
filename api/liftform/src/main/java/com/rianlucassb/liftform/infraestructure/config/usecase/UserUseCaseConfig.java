package com.rianlucassb.liftform.infraestructure.config.usecase;

import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.core.usecases.user.getcurrentuser.GetCurrentUserUseCase;
import com.rianlucassb.liftform.core.usecases.user.getcurrentuser.GetCurrentUserUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserUseCaseConfig {

    @Bean
    public GetCurrentUserUseCase getCurrentUserUseCase(
            UserRepository userRepository
    ) {
        return new GetCurrentUserUseCaseImpl(userRepository);
    }
}
