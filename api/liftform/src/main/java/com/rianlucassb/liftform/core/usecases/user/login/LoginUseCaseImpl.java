package com.rianlucassb.liftform.core.usecases.user.login;

import com.rianlucassb.liftform.core.domain.exception.InvalidCredentialsException;
import com.rianlucassb.liftform.core.domain.model.RefreshToken;
import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.gateway.security.*;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class LoginUseCaseImpl implements LoginUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final AccessTokenGenerator accessTokenGenerator;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenHasher refreshTokenHasher;

    public LoginUseCaseImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenGenerator refreshTokenGenerator,
            AccessTokenGenerator accessTokenGenerator, PasswordHasher passwordHasher, RefreshTokenHasher refreshTokenHasher
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.accessTokenGenerator = accessTokenGenerator;
        this.passwordHasher = passwordHasher;
        this.refreshTokenHasher = refreshTokenHasher;
    }

    public LoginUseCaseOutput execute(LoginUseCaseInput input) {
        User user = authenticateUser(input.login(), input.password());

        Instant now = Instant.now();

        Optional<RefreshToken> activeToken = refreshTokenRepository.findActiveByUserId(user.id());

        activeToken.ifPresent(token ->
                refreshTokenRepository.save(token.revoke())
        );

        String accessToken = accessTokenGenerator.generate(user);
        String rawRefreshToken = refreshTokenGenerator.generate();

        RefreshToken refreshToken = new RefreshToken(
                refreshTokenHasher.hash(rawRefreshToken),
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
        return passwordHasher.verify(raw, hashed);
    }
}