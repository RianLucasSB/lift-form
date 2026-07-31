package com.rianlucassb.liftform.core.usecases.analysis.confirmvideoupload;


import com.rianlucassb.liftform.core.domain.exception.EntityNotFoundException;
import com.rianlucassb.liftform.core.domain.exception.FileTooLargeException;
import com.rianlucassb.liftform.core.domain.exception.InvalidStatusTransitionException;
import com.rianlucassb.liftform.core.domain.exception.UnsupportedFileTypeException;
import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import com.rianlucassb.liftform.core.gateway.analysis.VideoObjectMetadata;
import com.rianlucassb.liftform.core.gateway.analysis.VideoStorage;
import com.rianlucassb.liftform.core.gateway.event.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConfirmVideoUploadUseCaseImplTest {

    private static final long MAX_SIZE_BYTES = 524_288_000L;
    private static final String ALLOWED_CONTENT_TYPE = "video/mp4";

    @Mock private EventPublisher eventPublisher;
    @Mock private VideoAnalysisRepository videoAnalysisRepository;
    @Mock private VideoStorage videoStorage;

    private ConfirmVideoUploadUseCaseImpl useCase;

    private final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ConfirmVideoUploadUseCaseImpl(
                eventPublisher,
                videoAnalysisRepository,
                videoStorage,
                MAX_SIZE_BYTES,
                ALLOWED_CONTENT_TYPE
        );
    }

    // ── Happy-path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should not throw any exception when input is valid")
    void shouldNotThrowAnyExceptionWhenInputIsValid() {
        // Arrange
        var input = createValidInput();
        var videoAnalysis = createValidVideoAnalysis();

        doReturn(Optional.of(videoAnalysis))
            .when(videoAnalysisRepository).findById(input.videoAnalysisId());
        doReturn(new VideoObjectMetadata(1024L, ALLOWED_CONTENT_TYPE))
            .when(videoStorage).getObjectMetadata(videoAnalysis.getVideoS3Key());

        // Assert
        assertThatNoException().isThrownBy(() -> useCase.execute(input));
    }

    @Test
    @DisplayName("Should persist the analysis with UPLOADED status when input is valid")
    void shouldPersistAnalysisWithUploadedStatusWhenInputIsValid() {
        // Arrange
        var input = createValidInput();
        var videoAnalysis = createValidVideoAnalysis();

        doReturn(Optional.of(videoAnalysis))
            .when(videoAnalysisRepository).findById(input.videoAnalysisId());
        doReturn(new VideoObjectMetadata(1024L, ALLOWED_CONTENT_TYPE))
            .when(videoStorage).getObjectMetadata(videoAnalysis.getVideoS3Key());

        // Act
        useCase.execute(input);

        // Assert
        verify(videoAnalysisRepository).save(videoAnalysis);
        assertThat(videoAnalysis.getStatus()).isEqualTo(VideoAnalysisStatus.UPLOADED);
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

    // ── Uploaded-file validation ─────────────────────────────────────────────

    @Test
    @DisplayName("Should delete the object and throw FileTooLargeException when uploaded file exceeds the max size")
    void shouldDeleteObjectAndThrowFileTooLargeExceptionWhenUploadedFileExceedsMaxSize() {
        // Arrange
        var input = createValidInput();
        var videoAnalysis = createValidVideoAnalysis();

        doReturn(Optional.of(videoAnalysis))
            .when(videoAnalysisRepository).findById(input.videoAnalysisId());
        doReturn(new VideoObjectMetadata(MAX_SIZE_BYTES + 1, ALLOWED_CONTENT_TYPE))
            .when(videoStorage).getObjectMetadata(videoAnalysis.getVideoS3Key());

        // Act
        Throwable thrown = catchThrowable(() -> useCase.execute(input));

        // Assert
        assertThat(thrown).isInstanceOf(FileTooLargeException.class);
        assertThat(videoAnalysis.getStatus()).isEqualTo(VideoAnalysisStatus.CREATED);
        verify(videoStorage).deleteObject(videoAnalysis.getVideoS3Key());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Should delete the object and throw UnsupportedFileTypeException when content type is not allowed")
    void shouldDeleteObjectAndThrowUnsupportedFileTypeExceptionWhenContentTypeIsNotAllowed() {
        // Arrange
        var input = createValidInput();
        var videoAnalysis = createValidVideoAnalysis();

        doReturn(Optional.of(videoAnalysis))
            .when(videoAnalysisRepository).findById(input.videoAnalysisId());
        doReturn(new VideoObjectMetadata(1024L, "video/quicktime"))
            .when(videoStorage).getObjectMetadata(videoAnalysis.getVideoS3Key());

        // Act
        Throwable thrown = catchThrowable(() -> useCase.execute(input));

        // Assert
        assertThat(thrown).isInstanceOf(UnsupportedFileTypeException.class);
        assertThat(videoAnalysis.getStatus()).isEqualTo(VideoAnalysisStatus.CREATED);
        verify(videoStorage).deleteObject(videoAnalysis.getVideoS3Key());
        verify(eventPublisher, never()).publish(any());
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
