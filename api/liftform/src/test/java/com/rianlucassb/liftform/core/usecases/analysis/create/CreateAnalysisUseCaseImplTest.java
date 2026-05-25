package com.rianlucassb.liftform.core.usecases.analysis.create;

import com.rianlucassb.liftform.core.domain.exception.VideoStoreException;
import com.rianlucassb.liftform.core.domain.exception.VideoUploadUrlGenerationException;
import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import com.rianlucassb.liftform.core.gateway.analysis.VideoStorage;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.util.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateAnalysisUseCaseImplTest {

    @Mock private VideoAnalysisRepository videoAnalysisRepository;
    @Mock private VideoStorage videoStorage;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CreateAnalysisUseCaseImpl createAnalysisUseCase;

    @BeforeEach
    public void beforeEach(){
        lenient().doReturn(Optional.of(TestFixtures.createUser())).when(userRepository).findById(any());
    }

    // ── Happy-path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return a non-null uploadUrl and videoAnalysisId when input is valid")
    void shouldReturnUploadURLAndAnalysisIDWhenInputIsValid() {
        // Arrange
        var input = createValidInput();

        doReturn(createValidVideoAnalysis()).when(videoAnalysisRepository).save(any(VideoAnalysis.class));
        doReturn("https://example.com/upload-url").when(videoStorage).generateUploadUrl(any(), any());

        // Act
        var output = createAnalysisUseCase.execute(input);

        // Assert
        assertThat(output.analysisId()).isNotNull();
        assertThat(output.uploadUrl()).isNotNull();
        assertThat(output.expiresIn()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should return expiresIn of exactly 900 seconds (15 minutes)")
    void shouldReturnExactly900SecondsAsExpiresIn() {
        var input = createValidInput();
        doReturn(createValidVideoAnalysis()).when(videoAnalysisRepository).save(any(VideoAnalysis.class));

        var output = createAnalysisUseCase.execute(input);

        assertThat(output.expiresIn()).isEqualTo(900);
    }

    @Test
    @DisplayName("Should save VideoAnalysis with userId and exerciseType from input")
    void shouldCallVideoAnalysisRepositoryWithCorrectInputFields() {
        // Arrange
        var input = createValidInput();
        doReturn(createValidVideoAnalysis()).when(videoAnalysisRepository).save(any(VideoAnalysis.class));

        createAnalysisUseCase.execute(input);

        ArgumentCaptor<VideoAnalysis> captor = ArgumentCaptor.forClass(VideoAnalysis.class);
        verify(videoAnalysisRepository).save(captor.capture());

        assertThat(captor.getValue().getUserId()).isEqualTo(UUID.fromString(input.userId()));
        assertThat(captor.getValue().getExerciseType()).isEqualTo(input.exerciseType());
    }

    @Test
    @DisplayName("Should save VideoAnalysis with CREATED status")
    void shouldSaveVideoAnalysisWithCreatedStatus() {
        var input = createValidInput();
        doReturn(createValidVideoAnalysis()).when(videoAnalysisRepository).save(any(VideoAnalysis.class));

        createAnalysisUseCase.execute(input);

        ArgumentCaptor<VideoAnalysis> captor = ArgumentCaptor.forClass(VideoAnalysis.class);
        verify(videoAnalysisRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(VideoAnalysisStatus.CREATED);
    }

    @Test
    @DisplayName("Should use the same S3 key for the VideoAnalysis entity and for generateUploadUrl")
    void shouldUseSameKeyForAnalysisEntityAndUploadUrl() {
        var input = createValidInput();
        doReturn(createValidVideoAnalysis()).when(videoAnalysisRepository).save(any(VideoAnalysis.class));

        createAnalysisUseCase.execute(input);

        ArgumentCaptor<VideoAnalysis> entityCaptor = ArgumentCaptor.forClass(VideoAnalysis.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        verify(videoAnalysisRepository).save(entityCaptor.capture());
        verify(videoStorage).generateUploadUrl(keyCaptor.capture(), any(Duration.class));

        assertThat(entityCaptor.getValue().getVideoS3Key()).isEqualTo(keyCaptor.getValue());
    }

    @Test
    @DisplayName("Should call generateUploadUrl with a key matching the expected pattern and 15-minute duration")
    void shouldCallVideoStorageWithCorrectKeyPatternAnd15MinDuration() {
        var input = createValidInput();
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        doReturn(createValidVideoAnalysis()).when(videoAnalysisRepository).save(any(VideoAnalysis.class));

        createAnalysisUseCase.execute(input);

        verify(videoStorage).generateUploadUrl(keyCaptor.capture(), durationCaptor.capture());

        assertThat(keyCaptor.getValue())
                .startsWith("videos/" + input.exerciseType() + "/" + input.userId())
                .endsWith(".mp4");
        assertThat(durationCaptor.getValue()).isEqualTo(Duration.ofMinutes(15));
    }

    // ── Invalid input ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw NullPointerException when userId is null")
    void shouldThrowWhenUserIdIsNull() {
        var input = new CreateAnalysisUseCaseInput(null, ExerciseType.SQUAT, "file.mp4");

        assertThatThrownBy(() -> createAnalysisUseCase.execute(input))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when userId is blank")
    void shouldThrowWhenUserIdIsBlank() {
        var input = new CreateAnalysisUseCaseInput("   ", ExerciseType.SQUAT, "file.mp4");

        assertThatThrownBy(() -> createAnalysisUseCase.execute(input))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when userId is not a valid UUID")
    void shouldThrowWhenUserIdIsNotAValidUUID() {
        var input = new CreateAnalysisUseCaseInput("not-a-uuid", ExerciseType.SQUAT, "file.mp4");

        assertThatThrownBy(() -> createAnalysisUseCase.execute(input))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw NullPointerException when exerciseType is null")
    void shouldThrowWhenExerciseTypeIsNull() {
        var input = new CreateAnalysisUseCaseInput(UUID.randomUUID().toString(), null, "file.mp4");

        assertThatThrownBy(() -> createAnalysisUseCase.execute(input))
                .isInstanceOf(NullPointerException.class);
    }

    // ── Infrastructure failure propagation ───────────────────────────────────
    @Test
    @DisplayName("Should propagate exception thrown by videoAnalysisRepository.save")
    void shouldPropagateExceptionFromRepository() {
        var input = createValidInput();
        doThrow(new VideoStoreException("DB unavailable")).when(videoAnalysisRepository).save(any(VideoAnalysis.class));

        assertThatThrownBy(() -> createAnalysisUseCase.execute(input))
                .isInstanceOf(VideoStoreException.class)
                .hasMessage("DB unavailable");
    }

    @Test
    @DisplayName("Should propagate exception thrown by videoStorage.generateUploadUrl")
    void shouldPropagateExceptionFromVideoStorage() {
        var input = createValidInput();
        doReturn(createValidVideoAnalysis()).when(videoAnalysisRepository).save(any(VideoAnalysis.class));
        doThrow(new VideoUploadUrlGenerationException("Storage unavailable")).when(videoStorage).generateUploadUrl(any(), any());

        assertThatThrownBy(() -> createAnalysisUseCase.execute(input))
                .isInstanceOf(VideoUploadUrlGenerationException.class)
                .hasMessage("Storage unavailable");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VideoAnalysis createValidVideoAnalysis() {
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


    private CreateAnalysisUseCaseInput createValidInput() {
        return new CreateAnalysisUseCaseInput(
                UUID.randomUUID().toString(),
                ExerciseType.SQUAT,
                "filename.mp4"
        );
    }
}