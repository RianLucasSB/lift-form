package com.rianlucassb.liftform.core.usecases.user.register;

import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.gateway.security.*;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.util.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
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
        doReturn("hashedpassword").when(passwordHasher).hash("testpassword");
        doReturn("generatedaccesstoken").when(accessTokenGenerator).generate(org.mockito.ArgumentMatchers.any());
        doReturn("generatedrefreshtoken").when(refreshTokenGenerator).generate();
    }

    @Test
    void shouldCreateUserWithSuccessWhenUserNotExists() {
        // Arrange
        String username = "testuser";
        String email = "test@gmail.com";
        String password = "testpassword";
        RegisterUseCaseInput input = new RegisterUseCaseInput(username, email, password);

        doReturn(Optional.empty()).when(userRepository).findByEmail(email);
        doReturn(Optional.empty()).when(userRepository).findByUsername(username);
        doReturn(TestFixtures.createUser()).when(userRepository).save(any(User.class));

        // Act
        var output = registerUseCase.execute(input);

        // Assert
        assertThat(output).isNotNull();
        assertThat(output.accessToken()).isEqualTo("generatedaccesstoken");
        assertThat(output.refreshToken()).isEqualTo("generatedrefreshtoken");

        // captura DEPOIS do execute
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.email()).isEqualTo(email);
        assertThat(savedUser.username()).isEqualTo(username);
        assertThat(savedUser.password()).isEqualTo("hashedpassword");
    }
}