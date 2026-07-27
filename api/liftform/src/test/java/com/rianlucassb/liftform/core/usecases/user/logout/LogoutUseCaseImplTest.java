package com.rianlucassb.liftform.core.usecases.user.logout;

import com.rianlucassb.liftform.core.domain.model.RefreshToken;
import com.rianlucassb.liftform.core.gateway.security.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseImplTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private LogoutUseCaseImpl useCase;

    private final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("Should revoke and save the active refresh token when one exists")
    void shouldRevokeActiveRefreshTokenWhenOneExists() {
        // Arrange
        var input = new LogoutUseCaseInput(USER_ID.toString());
        var activeToken = createRefreshToken(false);

        doReturn(Optional.of(activeToken))
                .when(refreshTokenRepository).findActiveByUserId(USER_ID);

        // Act
        useCase.execute(input);

        // Assert
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().revoked()).isTrue();
    }

    @Test
    @DisplayName("Should do nothing when there is no active refresh token")
    void shouldDoNothingWhenNoActiveRefreshTokenExists() {
        // Arrange
        var input = new LogoutUseCaseInput(USER_ID.toString());

        doReturn(Optional.empty())
                .when(refreshTokenRepository).findActiveByUserId(USER_ID);

        // Act
        useCase.execute(input);

        // Assert
        verify(refreshTokenRepository, never()).save(any());
    }

    private RefreshToken createRefreshToken(boolean revoked) {
        return new RefreshToken(
                "hashed-token",
                USER_ID,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                revoked
        );
    }
}
