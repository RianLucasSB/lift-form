package com.rianlucassb.liftform.core.usecases.user.refreshtoken;

import com.rianlucassb.liftform.core.domain.exception.InvalidRefreshTokenException;
import com.rianlucassb.liftform.core.domain.exception.UserNotFoundAuthException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.within;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenUseCaseImplTest {

    private static final String RAW_TOKEN        = "validrefreshtoken";
    private static final String HASHED_TOKEN     = "hashedToken";
    private static final String NEW_RAW_TOKEN    = "newRawRefreshToken";
    private static final String NEW_HASHED_TOKEN = "newHashedToken";
    private static final String ACCESS_TOKEN     = "accessToken";

    @Mock PasswordHasher         passwordHasher;
    @Mock RefreshTokenHasher     refreshTokenHasher;
    @Mock AccessTokenGenerator   accessTokenGenerator;
    @Mock RefreshTokenGenerator  refreshTokenGenerator;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock UserRepository         userRepository;

    @InjectMocks
    RefreshTokenUseCaseImpl refreshTokenUseCase;

    @Test
    @DisplayName("Should return new access token and refresh token when refresh token is valid")
    void shouldReturnNewAccessTokenAndRefreshTokenWhenRefreshTokenIsValid() {
        User user = TestFixtures.createUser();
        RefreshToken token = buildValidRefreshToken(user.id());

        stubHasher(RAW_TOKEN, HASHED_TOKEN);
        stubHasher(NEW_RAW_TOKEN, NEW_HASHED_TOKEN);
        stubRefreshTokenRepository(HASHED_TOKEN, token);
        stubUserRepository(user.id(), user);
        stubAccessTokenGenerator(user, ACCESS_TOKEN);
        stubRefreshTokenGenerator(NEW_RAW_TOKEN);

        var output = refreshTokenUseCase.execute(new RefreshTokenUseCaseInput(RAW_TOKEN));

        assertThat(output).isNotNull();
        assertThat(output.accessToken()).isNotNull();
        assertThat(output.refreshToken()).isNotNull();
    }

    @Test
    @DisplayName("Should save new refresh token when refresh token is valid")
    void shouldSaveNewRefreshTokenWhenRefreshTokenIsValid() {
        User user = TestFixtures.createUser();
        RefreshToken token = buildValidRefreshToken(user.id());

        stubHasher(RAW_TOKEN, HASHED_TOKEN);
        stubHasher(NEW_RAW_TOKEN, NEW_HASHED_TOKEN);
        stubRefreshTokenRepository(HASHED_TOKEN, token);
        stubUserRepository(user.id(), user);
        stubAccessTokenGenerator(user, ACCESS_TOKEN);
        stubRefreshTokenGenerator(NEW_RAW_TOKEN);

        refreshTokenUseCase.execute(new RefreshTokenUseCaseInput(RAW_TOKEN));

        assertNewRefreshTokenSaved(NEW_HASHED_TOKEN, user.id());
    }

    @Test
    @DisplayName("Should throw InvalidRefreshTokenException when refresh token is not found")
    void shouldThrowWhenRefreshTokenIsNotFound() {
        stubHasher(RAW_TOKEN, HASHED_TOKEN);
        stubRefreshTokenRepositoryEmpty(HASHED_TOKEN);

        Throwable thrown = catchThrowable(
                () -> refreshTokenUseCase.execute(new RefreshTokenUseCaseInput(RAW_TOKEN))
        );

        assertThat(thrown).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("Should throw InvalidRefreshTokenException when refresh token is revoked")
    void shouldThrowWhenRefreshTokenIsRevoked() {
        RefreshToken revoked = buildRevokedRefreshToken();

        stubHasher(RAW_TOKEN, HASHED_TOKEN);
        stubRefreshTokenRepository(HASHED_TOKEN, revoked);

        Throwable thrown = catchThrowable(
                () -> refreshTokenUseCase.execute(new RefreshTokenUseCaseInput(RAW_TOKEN))
        );

        assertThat(thrown).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("Should throw UserNotFoundAuthException when refresh token owner is not found")
    void shouldThrowWhenRefreshTokenOwnerIsNotFound() {
        RefreshToken token = buildValidRefreshToken(UUID.randomUUID());

        stubHasher(RAW_TOKEN, HASHED_TOKEN);
        stubRefreshTokenRepository(HASHED_TOKEN, token);
        stubUserRepositoryEmpty(token.userId());

        Throwable thrown = catchThrowable(
                () -> refreshTokenUseCase.execute(new RefreshTokenUseCaseInput(RAW_TOKEN))
        );

        assertThat(thrown).isInstanceOf(UserNotFoundAuthException.class);
    }

    @Test
    @DisplayName("Should throw InvalidRefreshTokenException when refresh token is expired")
    void shouldThrowWhenRefreshTokenIsExpired() {
        RefreshToken expired = buildExpiredRefreshToken();

        stubHasher(RAW_TOKEN, HASHED_TOKEN);
        stubRefreshTokenRepository(HASHED_TOKEN, expired);

        Throwable thrown = catchThrowable(
                () -> refreshTokenUseCase.execute(new RefreshTokenUseCaseInput(RAW_TOKEN))
        );

        assertThat(thrown).isInstanceOf(InvalidRefreshTokenException.class);
    }

    private void stubHasher(String input, String output) {
        doReturn(output).when(refreshTokenHasher).hash(input);
    }

    private void stubRefreshTokenGenerator(String token) {
        doReturn(token).when(refreshTokenGenerator).generate();
    }

    private void stubAccessTokenGenerator(User user, String token) {
        doReturn(token).when(accessTokenGenerator).generate(user);
    }

    private void stubUserRepository(UUID userId, User user) {
        doReturn(Optional.of(user)).when(userRepository).findById(userId);
    }

    private void stubUserRepositoryEmpty(UUID userId) {
        doReturn(Optional.empty()).when(userRepository).findById(userId);
    }

    private void stubRefreshTokenRepository(String hashedToken, RefreshToken token) {
        doReturn(Optional.of(token)).when(refreshTokenRepository).findByHashedToken(hashedToken);
    }

    private void stubRefreshTokenRepositoryEmpty(String hashedToken) {
        doReturn(Optional.empty()).when(refreshTokenRepository).findByHashedToken(hashedToken);
    }

    // =========================================================================
    // Object factories
    // =========================================================================

    private RefreshToken buildValidRefreshToken(UUID userId) {
        return new RefreshToken(
                HASHED_TOKEN,
                userId,
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().plus(10, ChronoUnit.DAYS),
                false
        );
    }

    private RefreshToken buildExpiredRefreshToken() {
        return new RefreshToken(
                HASHED_TOKEN,
                UUID.randomUUID(),
                Instant.now().minus(2, ChronoUnit.DAYS),
                Instant.now().minus(1, ChronoUnit.DAYS),
                false
        );
    }

    private RefreshToken buildRevokedRefreshToken() {
        return new RefreshToken(
                HASHED_TOKEN,
                UUID.randomUUID(),
                Instant.now(),
                Instant.now().plus(10, ChronoUnit.DAYS),
                true
        );
    }

    // =========================================================================
    // Assert helpers
    // =========================================================================

    private void assertNewRefreshTokenSaved(String expectedHash, UUID expectedUserId) {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());

        RefreshToken saved = captor.getAllValues().get(1);
        Instant now = Instant.now();

        assertThat(saved.userId()).isEqualTo(expectedUserId);
        assertThat(saved.tokenHash()).isEqualTo(expectedHash);
        assertThat(saved.createdAt()).isCloseTo(now, within(10, ChronoUnit.SECONDS));
        assertThat(saved.expiresAt()).isCloseTo(now.plus(10, ChronoUnit.DAYS), within(1, ChronoUnit.SECONDS));
    }
}