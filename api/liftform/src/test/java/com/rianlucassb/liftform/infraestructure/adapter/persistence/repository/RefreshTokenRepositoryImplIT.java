package com.rianlucassb.liftform.infraestructure.adapter.persistence.repository;

import com.rianlucassb.liftform.core.domain.model.RefreshToken;
import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.gateway.security.RefreshTokenRepository;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("RefreshTokenRepository Integration Tests")
class RefreshTokenRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(new User(null, "tokenuser", "token@example.com", "pwd", null));
    }

    private RefreshToken buildToken(String hash, boolean revoked) {
        return new RefreshToken(
                hash,
                savedUser.id(),
                Instant.now(),
                Instant.now().plus(10, ChronoUnit.DAYS),
                revoked
        );
    }

    // ------------------------------------------------------------------ save
    @Test
    @DisplayName("save() persists a refresh token")
    void save_persistsToken() {
        RefreshToken token = buildToken("hash-abc", false);
        refreshTokenRepository.save(token);

        Optional<RefreshToken> found = refreshTokenRepository.findByHashedToken("hash-abc");
        assertThat(found).isPresent();
        assertThat(found.get().userId()).isEqualTo(savedUser.id());
        assertThat(found.get().revoked()).isFalse();
    }

    // -------------------------------------------------- findByHashedToken
    @Test
    @DisplayName("findByHashedToken() returns empty for unknown hash")
    void findByHashedToken_unknown_returnsEmpty() {
        Optional<RefreshToken> found = refreshTokenRepository.findByHashedToken("nonexistent");

        assertThat(found).isEmpty();
    }

    // ----------------------------------------------------- findByUserId
    @Test
    @DisplayName("findByUserId() returns the token for the user")
    void findByUserId_existingUser_returnsToken() {
        refreshTokenRepository.save(buildToken("hash-user", false));

        Optional<RefreshToken> found = refreshTokenRepository.findByUserId(savedUser.id());

        assertThat(found).isPresent();
        assertThat(found.get().userId()).isEqualTo(savedUser.id());
    }

    @Test
    @DisplayName("findByUserId() returns empty for user with no token")
    void findByUserId_noToken_returnsEmpty() {
        User otherUser = userRepository.save(new User(null, "notoken", "notoken@example.com", "pwd", null));

        Optional<RefreshToken> found = refreshTokenRepository.findByUserId(otherUser.id());

        assertThat(found).isEmpty();
    }

    // ----------------------------------------------- findActiveByUserId
    @Test
    @DisplayName("findActiveByUserId() returns token when not revoked")
    void findActiveByUserId_activeToken_returnsToken() {
        refreshTokenRepository.save(buildToken("hash-active", false));

        Optional<RefreshToken> found = refreshTokenRepository.findActiveByUserId(savedUser.id());

        assertThat(found).isPresent();
        assertThat(found.get().revoked()).isFalse();
    }

    @Test
    @DisplayName("findActiveByUserId() returns empty when token is revoked")
    void findActiveByUserId_revokedToken_returnsEmpty() {
        refreshTokenRepository.save(buildToken("hash-revoked", true));

        Optional<RefreshToken> found = refreshTokenRepository.findActiveByUserId(savedUser.id());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findActiveByUserId() returns empty for unknown user")
    void findActiveByUserId_unknownUser_returnsEmpty() {
        Optional<RefreshToken> found = refreshTokenRepository.findActiveByUserId(UUID.randomUUID());

        assertThat(found).isEmpty();
    }
}

