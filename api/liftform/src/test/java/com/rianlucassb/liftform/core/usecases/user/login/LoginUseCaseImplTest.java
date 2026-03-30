package com.rianlucassb.liftform.core.usecases.user.login;

import com.rianlucassb.liftform.core.domain.exception.InvalidCredentialsException;
import com.rianlucassb.liftform.core.domain.model.RefreshToken;
import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.gateway.security.*;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.util.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.within;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseImplTest {

    @Mock
    UserRepository userRepository;
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

    @InjectMocks
    private LoginUseCaseImpl loginUseCase;

    @Test
    @DisplayName("Should return access token when username and password are valid")
    void shouldReturnAccessTokenWhenUserNameAndPasswordAreValid() {
        // Arrange
        var input = createValidLoginInput();

        User user = TestFixtures.createUser();
        doReturn(Optional.empty()).when(userRepository).findByEmail(input.login());
        doReturn(Optional.of(user)).when(userRepository).findByUsername(input.login());
        doReturn(true).when(passwordHasher).verify(input.password(), user.password());
        doReturn("validaccesstoken").when(accessTokenGenerator).generate(user);
        doReturn("validrefreshtoken").when(refreshTokenGenerator).generate();

        // Act
        var output = loginUseCase.execute(input);

        // Assert
        assertThat(output).isNotNull();
        assertThat(output.accessToken()).isEqualTo("validaccesstoken");
    }

    @Test
    @DisplayName("Should return access token when username and password are valid")
    void shouldReturnAccessTokenWhenEmailAndPasswordAreValid() {
        // Arrange
        var input = createValidLoginInput();

        User user = TestFixtures.createUser();
        doReturn(Optional.of(user)).when(userRepository).findByEmail(input.login());

        doReturn(true).when(passwordHasher).verify(input.password(), user.password());
        doReturn("validaccesstoken").when(accessTokenGenerator).generate(user);
        doReturn("validrefreshtoken").when(refreshTokenGenerator).generate();

        // Act
        var output = loginUseCase.execute(input);

        // Assert
        assertThat(output).isNotNull();
        assertThat(output.accessToken()).isEqualTo("validaccesstoken");
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when user does not exist")
    void shouldThrowInvalidCredentialsWhenUserNotExists(){
        // Arrange
        var input = createValidLoginInput();

        doReturn(Optional.empty()).when(userRepository).findByEmail(input.login());
        doReturn(Optional.empty()).when(userRepository).findByUsername(input.login());

        // Act
        Throwable thrown = catchThrowable(() -> loginUseCase.execute(input));

        // Assert
        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password is incorrect")
    void shouldThrowInvalidCredentialsWhenPasswordIsIncorrect(){
        // Arrange
        var input = createValidLoginInput();

        User user = TestFixtures.createUser();
        doReturn(Optional.of(user)).when(userRepository).findByEmail(input.login());
        doReturn(false).when(passwordHasher).verify(input.password(), user.password());

        // Act
        Throwable thrown = catchThrowable(() -> loginUseCase.execute(input));

        // Assert
        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("Should save refresh token with correct data when successful")
    void shouldSaveRefreshTokenWithCorrectDataWhenSuccessfull(){
        // Arrange
        var input = createValidLoginInput();

        User user = TestFixtures.createUser();
        doReturn(Optional.empty()).when(userRepository).findByEmail(input.login());
        doReturn(Optional.of(user)).when(userRepository).findByUsername(input.login());
        doReturn(true).when(passwordHasher).verify(input.password(), user.password());
        doReturn("validaccesstoken").when(accessTokenGenerator).generate(user);
        doReturn("validrefreshtoken").when(refreshTokenGenerator).generate();

        // Act
        var output = loginUseCase.execute(input);

        // Assert
        assertThat(output).isNotNull();
        assertThat(output.accessToken()).isEqualTo("validaccesstoken");

        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

        RefreshToken saved = refreshTokenCaptor.getValue();

        assertThat(saved.revoked()).isFalse();

        Instant now = Instant.now();
        Instant expectedExpiration = now.plus(10, ChronoUnit.DAYS);

        assertThat(saved.expiresAt())
                .isCloseTo(expectedExpiration, within(1, ChronoUnit.SECONDS));

    }


    private LoginUseCaseInput createValidLoginInput(){
        return new LoginUseCaseInput("validusername", "validpassword");
    }

}