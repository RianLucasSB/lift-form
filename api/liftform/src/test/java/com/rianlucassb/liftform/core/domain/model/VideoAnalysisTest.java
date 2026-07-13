package com.rianlucassb.liftform.core.domain.model;

import com.rianlucassb.liftform.core.domain.exception.InvalidStatusTransitionException;
import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;
import com.rianlucassb.liftform.core.domain.model.enums.PipelineStage;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VideoAnalysisTest {

    // ── markCompleted ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should transition an UPLOADED analysis to COMPLETED and attach the result")
    void shouldMarkCompletedFromUploaded() {
        var analysis = uploadedAnalysis();
        var result = AnalysisResult.score(analysis.getId(), "squat-v1", new BigDecimal("0.9"), Map.of(), Map.of());

        analysis.markCompleted(result);

        assertThat(analysis.getStatus()).isEqualTo(VideoAnalysisStatus.COMPLETED);
        assertThat(analysis.getResult()).isEqualTo(result);
    }

    @Test
    @DisplayName("Should update updatedAt when marking completed")
    void shouldUpdateTimestampWhenMarkingCompleted() {
        var analysis = uploadedAnalysis();
        var before = analysis.getUpdatedAt();
        var result = AnalysisResult.score(analysis.getId(), "squat-v1", new BigDecimal("0.9"), Map.of(), Map.of());

        analysis.markCompleted(result);

        assertThat(analysis.getUpdatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("Should throw InvalidStatusTransitionException when marking completed from a non-UPLOADED status")
    void shouldThrowWhenMarkingCompletedFromWrongStatus() {
        var analysis = createdAnalysis();
        var result = AnalysisResult.score(analysis.getId(), "squat-v1", new BigDecimal("0.9"), Map.of(), Map.of());

        assertThatThrownBy(() -> analysis.markCompleted(result))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Should throw InvalidStatusTransitionException on a duplicate markCompleted call (idempotency guard)")
    void shouldThrowOnDuplicateMarkCompleted() {
        var analysis = uploadedAnalysis();
        var result = AnalysisResult.score(analysis.getId(), "squat-v1", new BigDecimal("0.9"), Map.of(), Map.of());
        analysis.markCompleted(result);

        assertThatThrownBy(() -> analysis.markCompleted(result))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // ── recordFailure ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should transition an UPLOADED analysis to FAILED and append the error")
    void shouldRecordFailureFromUploaded() {
        var analysis = uploadedAnalysis();
        var error = new PipelineError(null, analysis.getId(), PipelineStage.EXTRACTION, "boom", "trace", null);

        analysis.recordFailure(error);

        assertThat(analysis.getStatus()).isEqualTo(VideoAnalysisStatus.FAILED);
        assertThat(analysis.getErrors()).containsExactly(error);
    }

    @Test
    @DisplayName("Should update updatedAt when recording a failure")
    void shouldUpdateTimestampWhenRecordingFailure() {
        var analysis = uploadedAnalysis();
        var before = analysis.getUpdatedAt();
        var error = new PipelineError(null, analysis.getId(), PipelineStage.SCORING, "boom", "trace", null);

        analysis.recordFailure(error);

        assertThat(analysis.getUpdatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("Should throw InvalidStatusTransitionException when recording a failure from a non-UPLOADED status")
    void shouldThrowWhenRecordingFailureFromWrongStatus() {
        var analysis = createdAnalysis();
        var error = new PipelineError(null, analysis.getId(), PipelineStage.EXTRACTION, "boom", "trace", null);

        assertThatThrownBy(() -> analysis.recordFailure(error))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Should throw InvalidStatusTransitionException on a duplicate recordFailure call (idempotency guard)")
    void shouldThrowOnDuplicateRecordFailure() {
        var analysis = uploadedAnalysis();
        var error = new PipelineError(null, analysis.getId(), PipelineStage.EXTRACTION, "boom", "trace", null);
        analysis.recordFailure(error);

        assertThatThrownBy(() -> analysis.recordFailure(error))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private VideoAnalysis createdAnalysis() {
        return VideoAnalysis.initialize(UUID.randomUUID(), ExerciseType.SQUAT, "videos/SQUAT/user/video.mp4");
    }

    private VideoAnalysis uploadedAnalysis() {
        return VideoAnalysis.reconstitute(
                1L,
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
