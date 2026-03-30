package com.rianlucassb.liftform.core.usecases.user.register;

import com.rianlucassb.liftform.core.domain.exception.AlreadyExistsException;
import com.rianlucassb.liftform.core.domain.model.RefreshToken;
import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.gateway.security.*;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class RegisterUseCaseImpl implements RegisterUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenHasher refreshTokenHasher;
    private final AccessTokenGenerator accessTokenGenerator;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenRepository refreshTokenRepository;

    public RegisterUseCaseImpl(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            RefreshTokenHasher refreshTokenHasher,
            AccessTokenGenerator accessTokenGenerator,
            RefreshTokenGenerator refreshTokenGenerator,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.refreshTokenHasher = refreshTokenHasher;
        this.accessTokenGenerator = accessTokenGenerator;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public RegisterUseCaseOutput execute(RegisterUseCaseInput registerUseCaseInput) {
        if(userRepository.findByEmail(registerUseCaseInput.email()).isPresent()) {
            throw new AlreadyExistsException("Email already in use");
        }

        if(userRepository.findByUsername(registerUseCaseInput.username()).isPresent()) {
            throw new AlreadyExistsException("Username already in use");
        }

        User user = new User(
                null,
                registerUseCaseInput.username(),
                registerUseCaseInput.email(),
                passwordHasher.hash(registerUseCaseInput.password()),
                Instant.now()
        );

        user = userRepository.save(user);

        String accessToken = accessTokenGenerator.generate(user);
        String rawRefreshToken = refreshTokenGenerator.generate();

        Instant now = Instant.now();

        RefreshToken refreshToken = new RefreshToken(
                refreshTokenHasher.hash(rawRefreshToken),
                user.id(),
                now,
                now.plus(10, ChronoUnit.DAYS),
                false
        );

        refreshTokenRepository.save(refreshToken);

        return new RegisterUseCaseOutput(accessToken, rawRefreshToken);
    }
}
