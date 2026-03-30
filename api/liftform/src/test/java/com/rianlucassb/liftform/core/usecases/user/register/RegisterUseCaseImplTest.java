package com.rianlucassb.liftform.core.usecases.user.register;

import com.rianlucassb.liftform.core.domain.exception.AlreadyExistsException;
import com.rianlucassb.liftform.core.domain.model.RefreshToken;
import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.gateway.security.*;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.util.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUseCaseImplTest {

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
    RegisterUseCaseImpl registerUseCase;

    @BeforeEach
    void setup() {
        // Setup any necessary data or configurations before all tests

    }

    @Test
    @DisplayName("Should create user with success when user not exists")
    void shouldCreateUserWithSuccessWhenUserNotExists() {
        // Arrange
        RegisterUseCaseInput input = TestFixtures.createRegisterInput();

        doReturn(Optional.empty()).when(userRepository).findByEmail(input.email());
        doReturn(Optional.empty()).when(userRepository).findByUsername(input.username());
        doReturn(TestFixtures.createUser()).when(userRepository).save(any(User.class));
        doReturn("hashedpassword").when(passwordHasher).hash("testpassword");
        doReturn("generatedaccesstoken").when(accessTokenGenerator).generate(org.mockito.ArgumentMatchers.any());
        doReturn("generatedrefreshtoken").when(refreshTokenGenerator).generate();

        // Act
        var output = registerUseCase.execute(input);

        // Assert
        assertThat(output).isNotNull();
        assertThat(output.accessToken()).isEqualTo("generatedaccesstoken");
        assertThat(output.refreshToken()).isEqualTo("generatedrefreshtoken");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.email()).isEqualTo(input.email());
        assertThat(savedUser.username()).isEqualTo(input.username());
        assertThat(savedUser.password()).isEqualTo("hashedpassword");
    }

    @Test
    @DisplayName("Should save refresh token with correct data when successful")
    void shouldSaveRefreshTokenWithCorrectDataWhenSuccessfull(){
        // Arrange
        RegisterUseCaseInput input = TestFixtures.createRegisterInput();

        doReturn(Optional.empty()).when(userRepository).findByEmail(input.email());
        doReturn(Optional.empty()).when(userRepository).findByUsername(input.username());
        doReturn(TestFixtures.createUser()).when(userRepository).save(any(User.class));
        doReturn("hashedpassword").when(passwordHasher).hash("testpassword");
        doReturn("generatedaccesstoken").when(accessTokenGenerator).generate(org.mockito.ArgumentMatchers.any());
        doReturn("generatedrefreshtoken").when(refreshTokenGenerator).generate();

        // Act
        var output = registerUseCase.execute(input);

        // Assert
        assertThat(output).isNotNull();
        assertThat(output.accessToken()).isEqualTo("generatedaccesstoken");

        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

        RefreshToken saved = refreshTokenCaptor.getValue();

        assertThat(saved.revoked()).isFalse();

        Instant now = Instant.now();
        Instant expectedExpiration = now.plus(10, ChronoUnit.DAYS);

        assertThat(saved.expiresAt())
                .isCloseTo(expectedExpiration, within(1, ChronoUnit.SECONDS));

    }

    @Test
    @DisplayName("Should throw exception on error")
    void shouldThrowExceptionOnerror(){
        // Arrange
        doThrow(new RuntimeException()).when(userRepository).save(any());
        var input = TestFixtures.createRegisterInput();

        // Act & Assert
        Throwable thrown = catchThrowable(() -> {
            registerUseCase.execute(input);
        });

        // Assert
        assertThat(thrown).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should throw AlreadyExistsException when email already in use")
    void shouldThrowAlreadyExistsExceptionWhenEmailAlreadyInUse() {
        // Arrange
        RegisterUseCaseInput input = TestFixtures.createRegisterInput();
        doReturn(Optional.of(TestFixtures.createUser())).when(userRepository).findByEmail(input.email());

        // Act
        Throwable thrown = catchThrowable(() -> {
            registerUseCase.execute(input);
        });

        // Assert
        assertThat(thrown).isInstanceOf(AlreadyExistsException.class);
    }

    @Test
    @DisplayName("Should throw AlreadyExistsException when username already in use")
    void shouldThrowAlreadyExistsExceptionWhenUsernameAlreadyInUse() {
        // Arrange
        RegisterUseCaseInput input = TestFixtures.createRegisterInput();

        doReturn(Optional.of(TestFixtures.createUser())).when(userRepository).findByUsername(input.username());

        // Act
        Throwable thrown = catchThrowable(() -> {
            registerUseCase.execute(input);
        });

        // Assert
        assertThat(thrown).isInstanceOf(AlreadyExistsException.class);
    }
}