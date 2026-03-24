package com.rianlucassb.liftform.core.usecases.user.register;

import com.rianlucassb.liftform.core.domain.exception.AlreadyExistsException;
import com.rianlucassb.liftform.core.domain.model.RefreshToken;
import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.gateway.security.AccessTokenGenerator;
import com.rianlucassb.liftform.core.gateway.security.Hasher;
import com.rianlucassb.liftform.core.gateway.security.RefreshTokenGenerator;
import com.rianlucassb.liftform.core.gateway.security.RefreshTokenRepository;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class RegisterUseCaseImpl implements RegisterUseCase {
    private final UserRepository userRepository;
    private final Hasher hasher;
    private final AccessTokenGenerator accessTokenGenerator;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenRepository refreshTokenRepository;

    public RegisterUseCaseImpl(
        UserRepository userRepository,
        Hasher hasher,
        AccessTokenGenerator accessTokenGenerator,
        RefreshTokenGenerator refreshTokenGenerator,
        RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.hasher = hasher;
        this.accessTokenGenerator = accessTokenGenerator;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public RegisterUseCaseOutput execute(RegisterUseCaseInput registerUseCaseInput) {
        if(userRepository.findByEmail(registerUseCaseInput.email()).isPresent()) {
            throw new AlreadyExistsException("Email already in use");
        }

        if(userRepository.findByUsername(registerUseCaseInput.userName()).isPresent()) {
            throw new AlreadyExistsException("Username already in use");
        }

        User user = new User(
                null,
                registerUseCaseInput.userName(),
                registerUseCaseInput.email(),
                hasher.hash(registerUseCaseInput.password()),
                Instant.now()
        );

        user = userRepository.save(user);

        String accessToken = accessTokenGenerator.generate(user);
        String rawRefreshToken = refreshTokenGenerator.generate();

        Instant now = Instant.now();

        RefreshToken refreshToken = new RefreshToken(
                hasher.hash(rawRefreshToken),
                user.id(),
                now.plus(10, ChronoUnit.DAYS),
                now,
                false
        );

        refreshTokenRepository.save(refreshToken);

        return new RegisterUseCaseOutput(accessToken, rawRefreshToken);
    }
}
