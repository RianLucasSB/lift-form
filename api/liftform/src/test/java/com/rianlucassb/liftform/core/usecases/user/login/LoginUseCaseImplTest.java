package com.rianlucassb.liftform.core.usecases.user.login;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseImplTest {
    @Test
    void testLoginSuccess() {
        // Given
        String username = "testuser";
        String password = "testpassword";
        LoginUseCaseInput input = new LoginUseCaseInput(username, password);

        // When
        LoginUseCaseOutput output = new LoginUseCaseOutput("access-token", "refresh-token");

        // Then
        assertNotNull(output);
        assertEquals("access-token", output.accessToken());
        assertEquals("refresh-token", output.refreshToken());
    }

}