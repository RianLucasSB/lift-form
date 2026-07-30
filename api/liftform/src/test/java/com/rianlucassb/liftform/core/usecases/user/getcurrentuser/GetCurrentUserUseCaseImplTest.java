package com.rianlucassb.liftform.core.usecases.user.getcurrentuser;

import com.rianlucassb.liftform.core.domain.exception.EntityNotFoundException;
import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserUseCaseImplTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private GetCurrentUserUseCaseImpl useCase;

    private final UUID USER_ID = UUID.randomUUID();

    // ── Happy-path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return username and email when user exists")
    void shouldReturnUsernameAndEmailWhenUserExists() {
        // Arrange
        var input = new GetCurrentUserUseCaseInput(USER_ID.toString());

        doReturn(Optional.of(createUser()))
                .when(userRepository).findById(USER_ID);

        // Act
        GetCurrentUserUseCaseOutput output = useCase.execute(input);

        // Assert
        assertThat(output.username()).isEqualTo("lifter42");
        assertThat(output.email()).isEqualTo("lifter42@example.com");
    }

    // ── Error paths ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw EntityNotFoundException when user is not found")
    void shouldThrowEntityNotFoundExceptionWhenUserIsNotFound() {
        // Arrange
        var input = new GetCurrentUserUseCaseInput(USER_ID.toString());

        doReturn(Optional.empty())
                .when(userRepository).findById(USER_ID);

        // Act
        Throwable thrown = catchThrowable(() -> useCase.execute(input));

        // Assert
        assertThat(thrown).isInstanceOf(EntityNotFoundException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User createUser() {
        return new User(
                USER_ID,
                "lifter42",
                "lifter42@example.com",
                "hashed-password",
                Instant.now()
        );
    }
}
