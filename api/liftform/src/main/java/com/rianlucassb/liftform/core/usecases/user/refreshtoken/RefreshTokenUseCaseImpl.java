package com.rianlucassb.liftform.core.usecases.user.refreshtoken;

import com.rianlucassb.liftform.core.domain.exception.InvalidCredentialsException;
import com.rianlucassb.liftform.core.domain.model.RefreshToken;
import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.gateway.security.AccessTokenGenerator;
import com.rianlucassb.liftform.core.gateway.security.RefreshTokenGenerator;
import com.rianlucassb.liftform.core.gateway.security.RefreshTokenHasher;
import com.rianlucassb.liftform.core.gateway.security.RefreshTokenRepository;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;

public class RefreshTokenUseCaseImpl implements RefreshTokenUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final AccessTokenGenerator accessTokenGenerator;
    private final UserRepository userRepository;

    public RefreshTokenUseCaseImpl(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenHasher refreshTokenHasher,
            RefreshTokenGenerator refreshTokenGenerator,
            AccessTokenGenerator accessTokenGenerator,
            UserRepository userRepository
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenHasher = refreshTokenHasher;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.accessTokenGenerator = accessTokenGenerator;
        this.userRepository = userRepository;
    }


    @Override
    public RefreshTokenUseCaseOutput execute(RefreshTokenUseCaseInput input) {
        RefreshToken foundRefreshToken = refreshTokenRepository.findByHashedToken(refreshTokenHasher.hash(input.refreshToken()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if(foundRefreshToken.revoked()) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        User user = userRepository
                .findById(foundRefreshToken.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String newRefreshTokenRaw = refreshTokenGenerator.generate();
        String accessToken = accessTokenGenerator.generate(user);

        return new RefreshTokenUseCaseOutput(accessToken, newRefreshTokenRaw);
    }
}
