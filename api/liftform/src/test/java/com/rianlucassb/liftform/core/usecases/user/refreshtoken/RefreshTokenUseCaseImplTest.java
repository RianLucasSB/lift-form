package com.rianlucassb.liftform.core.usecases.user.refreshtoken;

import com.rianlucassb.liftform.core.domain.exception.InvalidCredentialsException;
import com.rianlucassb.liftform.core.domain.model.RefreshToken;
import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.gateway.security.*;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.util.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenUseCaseImplTest {

    @Mock
    PasswordHasher passwordHasher;
    @Mock
    RefreshTokenHasher refreshTokenHasher;
    @Mock
    AccessTokenGenerator accessTokenGenerator;
    @Mock
    RefreshTokenGenerator refreshTokenGenerator;
    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    UserRepository userRepository;

    @InjectMocks
    RefreshTokenUseCaseImpl refreshTokenUseCase;

    @Test
    @DisplayName("Should return new access token and refresh token when refresh token is valid")
    void shouldReturnNewAccessTokenAndRefreshTokenWhenRefreshTokenIsValid() {
        // Arrange
        var validInput = new RefreshTokenUseCaseInput("validrefreshtoken");
        var validRefreshToken = new RefreshToken(
                refreshTokenHasher.hash(validInput.refreshToken()),
                UUID.randomUUID(),
                null,
                null,
                false
        );

        User user = TestFixtures.createUser();

        doReturn(Optional.of(user))
                .when(userRepository)
                .findById(validRefreshToken.userId());

        doReturn("hashedtoken")
                .when(refreshTokenHasher)
                .hash(validInput.refreshToken());

        doReturn("accessToken")
                .when(accessTokenGenerator)
                .generate(user);

        doReturn("refreshToken")
                .when(refreshTokenGenerator)
                .generate();

        doReturn(Optional.of(validRefreshToken))
                .when(refreshTokenRepository)
                .findByHashedToken(any());

        // Act
        var output = refreshTokenUseCase.execute(validInput);

        // Assert
        assertThat(output).isNotNull();

        assertThat(output.accessToken()).isNotNull();
        assertThat(output.refreshToken()).isNotNull();
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when refresh token is revoked")
    void shouldThrowInvalidCredentialsWhenRefreshTokenIsRevoked(){
        // Arrange
        var validInput = new RefreshTokenUseCaseInput("validrefreshtoken");
        var revokedRefreshToken = new RefreshToken(
                refreshTokenHasher.hash(validInput.refreshToken()),
                UUID.randomUUID(),
                null,
                null,
                true
        );

        doReturn("hashedtoken")
                .when(refreshTokenHasher)
                .hash(validInput.refreshToken());

        doReturn(Optional.of(revokedRefreshToken))
                .when(refreshTokenRepository)
                .findByHashedToken(any());

        // Act
        Throwable thrown = catchThrowable(() -> refreshTokenUseCase.execute(validInput));

        // Assert
        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class);
    }

}
