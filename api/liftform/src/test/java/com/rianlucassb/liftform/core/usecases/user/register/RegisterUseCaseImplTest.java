package com.rianlucassb.liftform.core.usecases.user.register;

import com.rianlucassb.liftform.core.domain.exception.AlreadyExistsException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUseCaseImplTest {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final String RAW_PASSWORD    = "testpassword";
    private static final String HASHED_PASSWORD = "hashedpassword";
    private static final String ACCESS_TOKEN    = "generatedaccesstoken";
    private static final String REFRESH_TOKEN   = "generatedrefreshtoken";

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
    RegisterUseCaseImpl registerUseCase;

    // =========================================================================
    // Tests
    // =========================================================================

    @Test
    @DisplayName("Should create user with success when user does not exist")
    void shouldCreateUserWhenUserNotExists() {
        RegisterUseCaseInput input = TestFixtures.createRegisterInput();
        stubHappyPath(input);

        var output = registerUseCase.execute(input);

        assertThat(output).isNotNull();
        assertThat(output.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(output.refreshToken()).isEqualTo(REFRESH_TOKEN);
        assertSavedUserFields(input);
    }

    @Test
    @DisplayName("Should save refresh token with correct data when registration is successful")
    void shouldSaveRefreshTokenWithCorrectDataWhenSuccessful() {
        RegisterUseCaseInput input = TestFixtures.createRegisterInput();
        stubHappyPath(input);

        registerUseCase.execute(input);

        assertRefreshTokenSaved();
    }

    @Test
    @DisplayName("Should throw RuntimeException when user repository save fails")
    void shouldThrowWhenRepositorySaveFails() {
        RegisterUseCaseInput input = TestFixtures.createRegisterInput();
        doThrow(new RuntimeException()).when(userRepository).save(any());

        Throwable thrown = catchThrowable(() -> registerUseCase.execute(input));

        assertThat(thrown).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should throw AlreadyExistsException when email is already in use")
    void shouldThrowWhenEmailAlreadyInUse() {
        RegisterUseCaseInput input = TestFixtures.createRegisterInput();
        stubUserRepositoryByEmail(input.email(), TestFixtures.createUser());

        Throwable thrown = catchThrowable(() -> registerUseCase.execute(input));

        assertThat(thrown).isInstanceOf(AlreadyExistsException.class);
    }

    @Test
    @DisplayName("Should throw AlreadyExistsException when username is already in use")
    void shouldThrowWhenUsernameAlreadyInUse() {
        RegisterUseCaseInput input = TestFixtures.createRegisterInput();
        stubUserRepositoryByUsername(input.username(), TestFixtures.createUser());

        Throwable thrown = catchThrowable(() -> registerUseCase.execute(input));

        assertThat(thrown).isInstanceOf(AlreadyExistsException.class);
    }

    // =========================================================================
    // Stub helpers
    // =========================================================================

    private void stubHappyPath(RegisterUseCaseInput input) {
        stubUserRepositoryEmptyByEmail(input.email());
        stubUserRepositoryEmptyByUsername(input.username());
        stubUserRepositorySave(TestFixtures.createUser());
        stubPasswordHasher(RAW_PASSWORD, HASHED_PASSWORD);
        stubAccessTokenGenerator(ACCESS_TOKEN);
        stubRefreshTokenGenerator(REFRESH_TOKEN);
    }

    private void stubUserRepositoryByEmail(String email, User user) {
        doReturn(Optional.of(user)).when(userRepository).findByEmail(email);
    }

    private void stubUserRepositoryEmptyByEmail(String email) {
        doReturn(Optional.empty()).when(userRepository).findByEmail(email);
    }

    private void stubUserRepositoryByUsername(String username, User user) {
        doReturn(Optional.of(user)).when(userRepository).findByUsername(username);
    }

    private void stubUserRepositoryEmptyByUsername(String username) {
        doReturn(Optional.empty()).when(userRepository).findByUsername(username);
    }

    private void stubUserRepositorySave(User user) {
        doReturn(user).when(userRepository).save(any(User.class));
    }

    private void stubPasswordHasher(String raw, String hashed) {
        doReturn(hashed).when(passwordHasher).hash(raw);
    }

    private void stubAccessTokenGenerator(String token) {
        doReturn(token).when(accessTokenGenerator).generate(any());
    }

    private void stubRefreshTokenGenerator(String token) {
        doReturn(token).when(refreshTokenGenerator).generate();
    }

    // =========================================================================
    // Assert helpers
    // =========================================================================

    private void assertSavedUserFields(RegisterUseCaseInput input) {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.email()).isEqualTo(input.email());
        assertThat(saved.username()).isEqualTo(input.username());
        assertThat(saved.password()).isEqualTo(HASHED_PASSWORD);
    }

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