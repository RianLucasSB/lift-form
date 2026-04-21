package com.rianlucassb.liftform.infraestructure.adapter.persistence.repository;

import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("UserRepository Integration Tests")
class UserRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    // ------------------------------------------------------------------ save
    @Test
    @DisplayName("save() persists a new user and returns it with same data")
    void save_persistsUser() {
        User user = new User(null, "johndoe", "john@example.com", "hashed_pwd", null);

        User saved = userRepository.save(user);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.username()).isEqualTo("johndoe");
        assertThat(saved.email()).isEqualTo("john@example.com");
        assertThat(saved.password()).isEqualTo("hashed_pwd");
    }

    // --------------------------------------------------------------- findById
    @Test
    @DisplayName("findById() returns the user when it exists")
    void findById_existingUser_returnsUser() {
        User saved = userRepository.save(new User(null, "alice", "alice@example.com", "pwd", null));

        Optional<User> found = userRepository.findById(saved.id());

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(saved.id());
    }

    @Test
    @DisplayName("findById() returns empty when user does not exist")
    void findById_nonExistingUser_returnsEmpty() {
        Optional<User> found = userRepository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    // --------------------------------------------------------- findByEmail
    @Test
    @DisplayName("findByEmail() returns user for exact email match")
    void findByEmail_existingEmail_returnsUser() {
        userRepository.save(new User(null, "bob", "bob@example.com", "pwd", null));

        Optional<User> found = userRepository.findByEmail("bob@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().email()).isEqualTo("bob@example.com");
    }

    @Test
    @DisplayName("findByEmail() is case-insensitive")
    void findByEmail_caseInsensitive_returnsUser() {
        userRepository.save(new User(null, "carol", "carol@example.com", "pwd", null));

        Optional<User> found = userRepository.findByEmail("CAROL@EXAMPLE.COM");

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("findByEmail() returns empty for unknown email")
    void findByEmail_unknownEmail_returnsEmpty() {
        Optional<User> found = userRepository.findByEmail("nobody@example.com");

        assertThat(found).isEmpty();
    }

    // ------------------------------------------------------ findByUsername
    @Test
    @DisplayName("findByUsername() returns user for matching username")
    void findByUsername_existingUsername_returnsUser() {
        userRepository.save(new User(null, "dave", "dave@example.com", "pwd", null));

        Optional<User> found = userRepository.findByUsername("dave");

        assertThat(found).isPresent();
        assertThat(found.get().username()).isEqualTo("dave");
    }

    @Test
    @DisplayName("findByUsername() is case-insensitive")
    void findByUsername_caseInsensitive_returnsUser() {
        userRepository.save(new User(null, "eve", "eve@example.com", "pwd", null));

        Optional<User> found = userRepository.findByUsername("EVE");

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("findByUsername() returns empty for unknown username")
    void findByUsername_unknownUsername_returnsEmpty() {
        Optional<User> found = userRepository.findByUsername("ghost");

        assertThat(found).isEmpty();
    }
}

