package com.rianlucassb.liftform.core.usecases.analysis.listanalyses;

import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class ListAnalysesUseCaseImplTest {

    @Mock private VideoAnalysisRepository videoAnalysisRepository;

    @InjectMocks
    private ListAnalysesUseCaseImpl useCase;

    private final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("Should return summaries of all user analyses in repository order")
    void shouldReturnSummariesOfAllUserAnalyses() {
        // Arrange
        var input = new ListAnalysesUseCaseInput(USER_ID.toString());
        var newer = createVideoAnalysis(2L, VideoAnalysisStatus.COMPLETED);
        var older = createVideoAnalysis(1L, VideoAnalysisStatus.FAILED);

        doReturn(List.of(newer, older))
                .when(videoAnalysisRepository).findAllByUserId(USER_ID);

        // Act
        ListAnalysesUseCaseOutput output = useCase.execute(input);

        // Assert
        assertThat(output.analyses()).containsExactly(
                new AnalysisSummary(2L, ExerciseType.SQUAT, VideoAnalysisStatus.COMPLETED, newer.getCreatedAt(), newer.getUpdatedAt()),
                new AnalysisSummary(1L, ExerciseType.SQUAT, VideoAnalysisStatus.FAILED, older.getCreatedAt(), older.getUpdatedAt())
        );
    }

    @Test
    @DisplayName("Should return empty list when user has no analyses")
    void shouldReturnEmptyListWhenUserHasNoAnalyses() {
        // Arrange
        var input = new ListAnalysesUseCaseInput(USER_ID.toString());

        doReturn(List.of())
                .when(videoAnalysisRepository).findAllByUserId(USER_ID);

        // Act
        ListAnalysesUseCaseOutput output = useCase.execute(input);

        // Assert
        assertThat(output.analyses()).isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VideoAnalysis createVideoAnalysis(Long id, VideoAnalysisStatus status) {
        return VideoAnalysis.reconstitute(
                id,
                USER_ID,
                ExerciseType.SQUAT,
                "videos/SQUAT/" + USER_ID + "/" + UUID.randomUUID() + ".mp4",
                status,
                Instant.now(),
                Instant.now(),
                null,
                new ArrayList<>()
        );
    }
}
