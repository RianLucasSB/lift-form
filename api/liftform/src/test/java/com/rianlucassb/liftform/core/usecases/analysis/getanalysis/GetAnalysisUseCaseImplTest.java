package com.rianlucassb.liftform.core.usecases.analysis.getanalysis;

import com.rianlucassb.liftform.core.domain.exception.EntityNotFoundException;
import com.rianlucassb.liftform.core.domain.model.AnalysisResult;
import com.rianlucassb.liftform.core.domain.model.PipelineError;
import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.domain.model.enums.Classification;
import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;
import com.rianlucassb.liftform.core.domain.model.enums.PipelineStage;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class GetAnalysisUseCaseImplTest {

    @Mock private VideoAnalysisRepository videoAnalysisRepository;

    @InjectMocks
    private GetAnalysisUseCaseImpl useCase;

    private final UUID USER_ID = UUID.randomUUID();

    // ── Happy-path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return status COMPLETED with result and no errors when analysis is completed")
    void shouldReturnStatusAndResultWhenAnalysisIsCompleted() {
        // Arrange
        var input = createValidInput();
        var result = createAnalysisResult();

        doReturn(Optional.of(createVideoAnalysis(VideoAnalysisStatus.COMPLETED, result, new ArrayList<>())))
                .when(videoAnalysisRepository).findByIdWithDetails(input.analysisId());

        // Act
        GetAnalysisUseCaseOutput output = useCase.execute(input);

        // Assert
        assertThat(output.status()).isEqualTo(VideoAnalysisStatus.COMPLETED);
        assertThat(output.result()).isEqualTo(result);
        assertThat(output.errors()).isEmpty();
    }

    @Test
    @DisplayName("Should return status with null result when analysis is still processing")
    void shouldReturnNullResultWhenAnalysisIsProcessing() {
        // Arrange
        var input = createValidInput();

        doReturn(Optional.of(createVideoAnalysis(VideoAnalysisStatus.PROCESSING, null, new ArrayList<>())))
                .when(videoAnalysisRepository).findByIdWithDetails(input.analysisId());

        // Act
        GetAnalysisUseCaseOutput output = useCase.execute(input);

        // Assert
        assertThat(output.status()).isEqualTo(VideoAnalysisStatus.PROCESSING);
        assertThat(output.result()).isNull();
        assertThat(output.errors()).isEmpty();
    }

    @Test
    @DisplayName("Should return errors and null result when analysis has failed")
    void shouldReturnErrorsWhenAnalysisHasFailed() {
        // Arrange
        var input = createValidInput();
        var error = createPipelineError();

        doReturn(Optional.of(createVideoAnalysis(VideoAnalysisStatus.FAILED, null, List.of(error))))
                .when(videoAnalysisRepository).findByIdWithDetails(input.analysisId());

        // Act
        GetAnalysisUseCaseOutput output = useCase.execute(input);

        // Assert
        assertThat(output.status()).isEqualTo(VideoAnalysisStatus.FAILED);
        assertThat(output.result()).isNull();
        assertThat(output.errors()).containsExactly(error);
    }

    // ── Error paths ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw EntityNotFoundException when analysis is not found")
    void shouldThrowEntityNotFoundExceptionWhenAnalysisIsNotFound() {
        // Arrange
        var input = createValidInput();

        doReturn(Optional.empty())
                .when(videoAnalysisRepository).findByIdWithDetails(input.analysisId());

        // Act
        Throwable thrown = catchThrowable(() -> useCase.execute(input));

        // Assert
        assertThat(thrown).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user is not the owner of the Analysis")
    void shouldThrowEntityNotFoundExceptionWhenUserIsNotTheOwner() {
        // Arrange
        var input = createValidInput();

        doReturn(Optional.of(createVideoAnalysisWithDifferentUserID()))
                .when(videoAnalysisRepository).findByIdWithDetails(input.analysisId());

        // Act
        Throwable thrown = catchThrowable(() -> useCase.execute(input));

        // Assert
        assertThat(thrown).isInstanceOf(EntityNotFoundException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VideoAnalysis createVideoAnalysis(VideoAnalysisStatus status, AnalysisResult result, List<PipelineError> errors) {
        return VideoAnalysis.reconstitute(
                1L,
                USER_ID,
                ExerciseType.SQUAT,
                "videos/SQUAT/" + USER_ID + "/" + UUID.randomUUID() + ".mp4",
                status,
                Instant.now(),
                Instant.now(),
                result,
                new ArrayList<>(errors)
        );
    }

    private VideoAnalysis createVideoAnalysisWithDifferentUserID() {
        UUID userId = UUID.randomUUID();
        return VideoAnalysis.reconstitute(
                1L,
                userId,
                ExerciseType.SQUAT,
                "videos/SQUAT/" + userId + "/" + UUID.randomUUID() + ".mp4",
                VideoAnalysisStatus.COMPLETED,
                Instant.now(),
                Instant.now(),
                null,
                new ArrayList<>()
        );
    }

    private AnalysisResult createAnalysisResult() {
        return new AnalysisResult(
                10L,
                1L,
                "v1",
                new BigDecimal("0.85"),
                Classification.GOOD,
                Map.of("overall_label", "excellent"),
                Map.of("depth_mean", 0.42),
                Instant.now()
        );
    }

    private PipelineError createPipelineError() {
        return new PipelineError(
                20L,
                1L,
                PipelineStage.SCORING,
                "Scoring failed: invalid feature vector",
                "Traceback (most recent call last): ...",
                Instant.now()
        );
    }

    private GetAnalysisUseCaseInput createValidInput() {
        return new GetAnalysisUseCaseInput(
                1L,
                USER_ID.toString()
        );
    }
}
