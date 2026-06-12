package com.rianlucassb.liftform.core.usecases.analysis.confirmvideoupload;


import com.rianlucassb.liftform.core.domain.exception.EntityNotFoundException;
import com.rianlucassb.liftform.core.domain.exception.InvalidStatusTransitionException;
import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import com.rianlucassb.liftform.core.gateway.event.EventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class ConfirmVideoUploadUseCaseImplTest {

    @Mock private EventPublisher eventPublisher;
    @Mock private VideoAnalysisRepository videoAnalysisRepository;

    @InjectMocks
    private ConfirmVideoUploadUseCaseImpl useCase;

    private final UUID USER_ID = UUID.randomUUID();

    // ── Happy-path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should not throw any exception when input is valid")
    void shouldNotThrowAnyExceptionWhenInputIsValid() {
        // Arrange
        var input = createValidInput();

        // Act
        doReturn(Optional.of(createValidVideoAnalysis()))
            .when(videoAnalysisRepository).findById(input.videoAnalysisId());

        // Assert
        assertThatNoException().isThrownBy(() -> useCase.execute(input));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when analysis is not found")
    void shouldThrowEntityNotFoundExceptionWhenAnalysisIsNotFound() {
        // Arrange
        var input = createValidInput();

        // Act
        doReturn(Optional.empty())
                .when(videoAnalysisRepository).findById(input.videoAnalysisId());

        Throwable thrown = catchThrowable(() -> useCase.execute(input));

        // Assert
        assertThat(thrown).isInstanceOf(EntityNotFoundException.class);
    }


    @Test
    @DisplayName("Should throw InvalidStatusTransitionException when analysis status is invalid")
    void shouldThrowInvalidStatusTransitionExceptionWhenAnalysisStatusIsInvalid() {
        // Arrange
        var input = createValidInput();

        // Act
        doReturn(Optional.of(createVideoAnalysisWithInvalidStatus()))
                .when(videoAnalysisRepository).findById(input.videoAnalysisId());

        Throwable thrown = catchThrowable(() -> useCase.execute(input));

        // Assert
        assertThat(thrown).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user is not the owner of the Analysis")
    void shouldThrowEntityNotFoundExceptionWhenUserIsNotTheOwner() {
        // Arrange
        var input = createValidInput();

        // Act
        doReturn(Optional.of(createVideoAnalysisWithDifferentUserID()))
                .when(videoAnalysisRepository).findById(input.videoAnalysisId());

        Throwable thrown = catchThrowable(() -> useCase.execute(input));

        // Assert
        assertThat(thrown).isInstanceOf(EntityNotFoundException.class);
    }


    // ── Helpers ───────────────────────────────────────────────────────────────

    private VideoAnalysis createValidVideoAnalysis() {
        return VideoAnalysis.reconstitute(
                1L,
                USER_ID,
                ExerciseType.SQUAT,
                "videos/SQUAT/" + USER_ID + "/" + UUID.randomUUID() + ".mp4",
                VideoAnalysisStatus.CREATED,
                null,
                null,
                null,
                new ArrayList<>()
        );
    }

    private VideoAnalysis createVideoAnalysisWithInvalidStatus() {
        return VideoAnalysis.reconstitute(
                1L,
                USER_ID,
                ExerciseType.SQUAT,
                "videos/SQUAT/" + USER_ID + "/" + UUID.randomUUID() + ".mp4",
                VideoAnalysisStatus.EXPIRED,
                null,
                null,
                null,
                new ArrayList<>()
        );
    }

    private VideoAnalysis createVideoAnalysisWithDifferentUserID() {
        UUID userId = UUID.randomUUID();
        return VideoAnalysis.reconstitute(
                1L,
                userId,
                ExerciseType.SQUAT,
                "videos/SQUAT/" + userId + "/" + UUID.randomUUID() + ".mp4",
                VideoAnalysisStatus.CREATED,
                null,
                null,
                null,
                new ArrayList<>()
        );
    }

    private ConfirmVideoUploadUseCaseInput createValidInput() {
        return new ConfirmVideoUploadUseCaseInput(
                1L,
                USER_ID.toString()
        );
    }
}