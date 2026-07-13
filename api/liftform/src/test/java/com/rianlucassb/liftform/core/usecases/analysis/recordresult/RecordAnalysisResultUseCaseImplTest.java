package com.rianlucassb.liftform.core.usecases.analysis.recordresult;

import com.rianlucassb.liftform.core.domain.exception.EntityNotFoundException;
import com.rianlucassb.liftform.core.domain.exception.VideoStoreException;
import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.domain.model.enums.Classification;
import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordAnalysisResultUseCaseImplTest {

    @Mock private VideoAnalysisRepository videoAnalysisRepository;

    @InjectMocks
    private RecordAnalysisResultUseCaseImpl recordAnalysisResultUseCase;

    @Test
    @DisplayName("Should mark the analysis completed with a derived classification and save it")
    void shouldMarkCompletedAndSave() {
        var analysis = uploadedAnalysis(1L);
        doReturn(Optional.of(analysis)).when(videoAnalysisRepository).findById(1L);
        doReturn(analysis).when(videoAnalysisRepository).save(any(VideoAnalysis.class));

        var input = new RecordAnalysisResultUseCaseInput(
                1L, "squat-v1", new BigDecimal("0.82"), Map.of("depth", "good"), Map.of("knee_bottom_avg", 48.0)
        );

        recordAnalysisResultUseCase.execute(input);

        ArgumentCaptor<VideoAnalysis> captor = ArgumentCaptor.forClass(VideoAnalysis.class);
        verify(videoAnalysisRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(VideoAnalysisStatus.COMPLETED);
        assertThat(captor.getValue().getResult().classification()).isEqualTo(Classification.GOOD);
        assertThat(captor.getValue().getResult().modelVersion()).isEqualTo("squat-v1");
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when the analysis does not exist")
    void shouldThrowWhenAnalysisNotFound() {
        doReturn(Optional.empty()).when(videoAnalysisRepository).findById(1L);

        var input = new RecordAnalysisResultUseCaseInput(
                1L, "squat-v1", new BigDecimal("0.82"), Map.of(), Map.of()
        );

        assertThatThrownBy(() -> recordAnalysisResultUseCase.execute(input))
                .isInstanceOf(EntityNotFoundException.class);

        verify(videoAnalysisRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should propagate exception thrown by videoAnalysisRepository.save")
    void shouldPropagateExceptionFromRepository() {
        var analysis = uploadedAnalysis(1L);
        doReturn(Optional.of(analysis)).when(videoAnalysisRepository).findById(1L);
        doThrow(new VideoStoreException("DB unavailable")).when(videoAnalysisRepository).save(any(VideoAnalysis.class));

        var input = new RecordAnalysisResultUseCaseInput(
                1L, "squat-v1", new BigDecimal("0.82"), Map.of(), Map.of()
        );

        assertThatThrownBy(() -> recordAnalysisResultUseCase.execute(input))
                .isInstanceOf(VideoStoreException.class)
                .hasMessage("DB unavailable");
    }

    private VideoAnalysis uploadedAnalysis(Long id) {
        return VideoAnalysis.reconstitute(
                id,
                UUID.randomUUID(),
                ExerciseType.SQUAT,
                "videos/SQUAT/user/video.mp4",
                VideoAnalysisStatus.UPLOADED,
                Instant.now(),
                Instant.now(),
                null,
                new ArrayList<>()
        );
    }
}
