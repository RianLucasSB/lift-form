package com.rianlucassb.liftform.infraestructure.config;

import com.rianlucassb.liftform.core.gateway.security.AccessTokenGenerator;
import com.rianlucassb.liftform.core.gateway.security.Hasher;
import com.rianlucassb.liftform.core.gateway.security.RefreshTokenGenerator;
import com.rianlucassb.liftform.core.gateway.security.RefreshTokenRepository;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.core.usecases.user.login.LoginUseCase;
import com.rianlucassb.liftform.core.usecases.user.login.LoginUseCaseImpl;
import com.rianlucassb.liftform.core.usecases.user.register.RegisterUseCase;
import com.rianlucassb.liftform.core.usecases.user.register.RegisterUseCaseImpl;
import com.rianlucassb.liftform.infraestructure.transaction.TransactionalLoginUseCase;
import com.rianlucassb.liftform.infraestructure.transaction.TransactionalRegisterUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class UseCaseConfig {

    @Bean("registerUseCaseImpl")
    public RegisterUseCase registerUseCase(
            UserRepository userRepository,
            Hasher hasher,
            AccessTokenGenerator accessTokenGenerator,
            RefreshTokenGenerator refreshTokenGenerator,
            RefreshTokenRepository refreshTokenRepository
    ) {
        return new RegisterUseCaseImpl(
                userRepository,
                hasher,
                accessTokenGenerator,
                refreshTokenGenerator,
                refreshTokenRepository
        );
    }

    @Bean
    @Primary
    public RegisterUseCase transactionalRegisterUseCase(
            @Qualifier("registerUseCaseImpl") RegisterUseCase registerUseCase
    ) {
        return new TransactionalRegisterUseCase(registerUseCase);
    }

    @Bean("loginUseCaseImpl")
    public LoginUseCase loginUseCase(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenGenerator refreshTokenGenerator,
            AccessTokenGenerator accessTokenGenerator,
            Hasher hasher
    ) {
        return new LoginUseCaseImpl(
                userRepository,
                refreshTokenRepository,
                refreshTokenGenerator,
                accessTokenGenerator,
                hasher
        );
    }

    @Bean
    @Primary
    public LoginUseCase transactionalLoginUseCase(
            @Qualifier("loginUseCaseImpl") LoginUseCase loginUseCase
    ) {
        return new TransactionalLoginUseCase(loginUseCase);
    }
}