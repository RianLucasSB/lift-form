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
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.within;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseImplTest {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final String LOGIN           = "validusername";
    private static final String PASSWORD        = "validpassword";
    private static final String ACCESS_TOKEN    = "validaccesstoken";
    private static final String REFRESH_TOKEN   = "validrefreshtoken";

    // -------------------------------------------------------------------------
    // Mocks & Subject
    // -------------------------------------------------------------------------

    @Mock UserRepository         userRepository;
    @Mock PasswordHasher         passwordHasher;
    @Mock RefreshTokenHasher     refreshTokenHasher;
    @Mock AccessTokenGenerator   accessTokenGenerator;
    @Mock RefreshTokenGenerator  refreshTokenGenerator;
    @Mock RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    LoginUseCaseImpl loginUseCase;

    // =========================================================================
    // Tests
    // =========================================================================

    @Test
    @DisplayName("Should return access token when username and password are valid")
    void shouldReturnAccessTokenWhenUsernameAndPasswordAreValid() {
        User user = TestFixtures.createUser();

        stubUserRepositoryEmptyByEmail(LOGIN);
        stubUserRepositoryByUsername(LOGIN, user);
        stubPasswordVerify(PASSWORD, user.password(), true);
        stubAccessTokenGenerator(user, ACCESS_TOKEN);
        stubRefreshTokenGenerator(REFRESH_TOKEN);

        var output = loginUseCase.execute(validInput());

        assertThat(output).isNotNull();
        assertThat(output.accessToken()).isEqualTo(ACCESS_TOKEN);
    }

    @Test
    @DisplayName("Should return access token when email and password are valid")
    void shouldReturnAccessTokenWhenEmailAndPasswordAreValid() {
        User user = TestFixtures.createUser();

        stubUserRepositoryByEmail(LOGIN, user);
        stubPasswordVerify(PASSWORD, user.password(), true);
        stubAccessTokenGenerator(user, ACCESS_TOKEN);
        stubRefreshTokenGenerator(REFRESH_TOKEN);

        var output = loginUseCase.execute(validInput());

        assertThat(output).isNotNull();
        assertThat(output.accessToken()).isEqualTo(ACCESS_TOKEN);
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when user does not exist")
    void shouldThrowWhenUserNotExists() {
        stubUserRepositoryEmptyByEmail(LOGIN);
        stubUserRepositoryEmptyByUsername(LOGIN);

        Throwable thrown = catchThrowable(() -> loginUseCase.execute(validInput()));

        assertThat(thrown)
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password is incorrect")
    void shouldThrowWhenPasswordIsIncorrect() {
        User user = TestFixtures.createUser();

        stubUserRepositoryByEmail(LOGIN, user);
        stubPasswordVerify(PASSWORD, user.password(), false);

        Throwable thrown = catchThrowable(() -> loginUseCase.execute(validInput()));

        assertThat(thrown)
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("Should save refresh token with correct data when login is successful")
    void shouldSaveRefreshTokenWithCorrectDataWhenSuccessful() {
        User user = TestFixtures.createUser();

        stubUserRepositoryEmptyByEmail(LOGIN);
        stubUserRepositoryByUsername(LOGIN, user);
        stubPasswordVerify(PASSWORD, user.password(), true);
        stubAccessTokenGenerator(user, ACCESS_TOKEN);
        stubRefreshTokenGenerator(REFRESH_TOKEN);

        loginUseCase.execute(validInput());

        assertRefreshTokenSaved();
    }

    // =========================================================================
    // Stub helpers
    // =========================================================================

    private void stubUserRepositoryByEmail(String login, User user) {
        doReturn(Optional.of(user)).when(userRepository).findByEmail(login);
    }

    private void stubUserRepositoryEmptyByEmail(String login) {
        doReturn(Optional.empty()).when(userRepository).findByEmail(login);
    }

    private void stubUserRepositoryByUsername(String login, User user) {
        doReturn(Optional.of(user)).when(userRepository).findByUsername(login);
    }

    private void stubUserRepositoryEmptyByUsername(String login) {
        doReturn(Optional.empty()).when(userRepository).findByUsername(login);
    }

    private void stubPasswordVerify(String raw, String hashed, boolean result) {
        doReturn(result).when(passwordHasher).verify(raw, hashed);
    }

    private void stubAccessTokenGenerator(User user, String token) {
        doReturn(token).when(accessTokenGenerator).generate(user);
    }

    private void stubRefreshTokenGenerator(String token) {
        doReturn(token).when(refreshTokenGenerator).generate();
    }

    // =========================================================================
    // Input factories
    // =========================================================================

    private LoginUseCaseInput validInput() {
        return new LoginUseCaseInput(LOGIN, PASSWORD);
    }

    // =========================================================================
    // Assert helpers
    // =========================================================================

    private void assertRefreshTokenSaved() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        Instant now = Instant.now();

        assertThat(saved.revoked()).isFalse();
        assertThat(saved.expiresAt())
                .isCloseTo(now.plus(10, ChronoUnit.DAYS), within(1, ChronoUnit.SECONDS));
    }
}