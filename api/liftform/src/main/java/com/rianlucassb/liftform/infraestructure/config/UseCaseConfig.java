package com.rianlucassb.liftform.infraestructure.config;

import com.rianlucassb.liftform.core.gateway.security.*;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.core.usecases.user.login.LoginUseCase;
import com.rianlucassb.liftform.core.usecases.user.login.LoginUseCaseImpl;
import com.rianlucassb.liftform.core.usecases.user.refreshtoken.RefreshTokenUseCase;
import com.rianlucassb.liftform.core.usecases.user.refreshtoken.RefreshTokenUseCaseImpl;
import com.rianlucassb.liftform.core.usecases.user.register.RegisterUseCase;
import com.rianlucassb.liftform.core.usecases.user.register.RegisterUseCaseImpl;
import com.rianlucassb.liftform.infraestructure.transaction.TransactionalLoginUseCase;
import com.rianlucassb.liftform.infraestructure.transaction.TransactionalRefreshTokenUseCase;
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
            PasswordHasher passwordHasher,
            RefreshTokenHasher refreshTokenHasher,
            AccessTokenGenerator accessTokenGenerator,
            RefreshTokenGenerator refreshTokenGenerator,
            RefreshTokenRepository refreshTokenRepository
    ) {
        return new RegisterUseCaseImpl(
                userRepository,
                passwordHasher,
                refreshTokenHasher,
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
            PasswordHasher passwordHasher,
            RefreshTokenHasher refreshTokenHasher
            ) {
        return new LoginUseCaseImpl(
                userRepository,
                refreshTokenRepository,
                refreshTokenGenerator,
                accessTokenGenerator,
                passwordHasher,
                refreshTokenHasher
        );
    }

    @Bean
    @Primary
    public LoginUseCase transactionalLoginUseCase(
            @Qualifier("loginUseCaseImpl") LoginUseCase loginUseCase
    ) {
        return new TransactionalLoginUseCase(loginUseCase);
    }

    @Bean("refreshTokenUseCaseImpl")
    public RefreshTokenUseCase refreshTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenHasher refreshTokenHasher,
            RefreshTokenGenerator refreshTokenGenerator,
            AccessTokenGenerator accessTokenGenerator,
            UserRepository userRepository
    ) {
        return new RefreshTokenUseCaseImpl(
                refreshTokenRepository,
                refreshTokenHasher,
                refreshTokenGenerator,
                accessTokenGenerator,
                userRepository
        );
    }

    @Bean
    @Primary
    public RefreshTokenUseCase transactionalRefreshTokenUseCase(
            @Qualifier("refreshTokenUseCaseImpl") RefreshTokenUseCase refreshTokenUseCase
    ) {
        return new TransactionalRefreshTokenUseCase(refreshTokenUseCase);
    }
}