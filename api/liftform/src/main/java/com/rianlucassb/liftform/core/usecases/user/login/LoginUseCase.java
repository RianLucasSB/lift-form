package com.rianlucassb.liftform.core.usecases.user.login;

import com.rianlucassb.liftform.core.domain.exception.InvalidCredentialsException;
import com.rianlucassb.liftform.core.domain.model.RefreshToken;
import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.gateway.security.AccessTokenGenerator;
import com.rianlucassb.liftform.core.gateway.security.Hasher;
import com.rianlucassb.liftform.core.gateway.security.RefreshTokenGenerator;
import com.rianlucassb.liftform.core.gateway.security.RefreshTokenRepository;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.core.usecases.UseCase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class LoginUseCase implements UseCase<LoginUseCaseInput, LoginUseCaseOutput> {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final AccessTokenGenerator accessTokenGenerator;
    private final Hasher hasher;

    public LoginUseCase(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, RefreshTokenGenerator refreshTokenGenerator, AccessTokenGenerator accessTokenGenerator, Hasher hasher) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.accessTokenGenerator = accessTokenGenerator;
        this.hasher = hasher;
    }

    public LoginUseCaseOutput execute(LoginUseCaseInput input) {

        User user = authenticateUser(input.login(), input.password());

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

        return new LoginUseCaseOutput(accessToken, rawRefreshToken);
    }

    private User authenticateUser(String login, String password) {
        User user = userRepository.findByEmail(login)
                .or(() -> userRepository.findByUsername(login))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordMatches(password, user.password())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        return user;
    }

    private boolean passwordMatches(String raw, String hashed) {
        return hasher.verify(raw, hashed);
    }
}