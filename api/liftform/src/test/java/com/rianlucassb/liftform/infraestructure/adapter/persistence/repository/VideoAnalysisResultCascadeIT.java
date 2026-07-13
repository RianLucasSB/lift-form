package com.rianlucassb.liftform.infraestructure.adapter.persistence.repository;

import com.rianlucassb.liftform.AbstractIntegrationTest;
import com.rianlucassb.liftform.core.domain.model.AnalysisResult;
import com.rianlucassb.liftform.core.domain.model.PipelineError;
import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;
import com.rianlucassb.liftform.core.domain.model.enums.PipelineStage;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("VideoAnalysis aggregate cascade Integration Tests")
class VideoAnalysisResultCascadeIT extends AbstractIntegrationTest {

    @Autowired
    private VideoAnalysisRepository videoAnalysisRepository;

    @Autowired
    private VideoAnalysisJpaRepository videoAnalysisJpaRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(new User(null, "analyst", "analyst@example.com", "pwd", null));
    }

    @Test
    @DisplayName("save() cascades a newly attached AnalysisResult into TB_ANALYSIS_RESULTS")
    void save_cascadesAnalysisResult() {
        VideoAnalysis uploaded = videoAnalysisRepository.save(uploadedAnalysis());

        var result = AnalysisResult.score(
                uploaded.getId(), "squat-v1", new BigDecimal("0.9"),
                Map.of("depth", "good"), Map.of("knee_bottom_avg", 48.0)
        );
        uploaded.markCompleted(result);
        videoAnalysisRepository.save(uploaded);

        var reloaded = videoAnalysisJpaRepository.findById(uploaded.getId()).orElseThrow();

        assertThat(reloaded.getResult()).isNotNull();
        assertThat(reloaded.getResult().getModelVersion()).isEqualTo("squat-v1");
        assertThat(reloaded.getResult().getOverallScore()).isEqualByComparingTo("0.9");
    }

    @Test
    @DisplayName("save() cascades a newly appended PipelineError into TB_PIPELINE_ERRORS")
    void save_cascadesPipelineError() {
        VideoAnalysis uploaded = videoAnalysisRepository.save(uploadedAnalysis());

        var error = new PipelineError(null, uploaded.getId(), PipelineStage.SCORING, "boom", "trace", null);
        uploaded.recordFailure(error);
        videoAnalysisRepository.save(uploaded);

        var reloaded = videoAnalysisJpaRepository.findById(uploaded.getId()).orElseThrow();

        assertThat(reloaded.getErrors()).hasSize(1);
        assertThat(reloaded.getErrors().getFirst().getStage()).isEqualTo(PipelineStage.SCORING);
        assertThat(reloaded.getErrors().getFirst().getErrorMessage()).isEqualTo("boom");
    }

    private VideoAnalysis uploadedAnalysis() {
        return VideoAnalysis.reconstitute(
                null,
                savedUser.id(),
                ExerciseType.SQUAT,
                "videos/SQUAT/" + savedUser.id() + "/cascade.mp4",
                VideoAnalysisStatus.UPLOADED,
                Instant.now(),
                Instant.now(),
                null,
                new ArrayList<>()
        );
    }
}
