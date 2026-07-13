package com.rianlucassb.liftform.core.domain.model;

import com.rianlucassb.liftform.core.domain.model.enums.Classification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisResultTest {

    @Test
    @DisplayName("Should derive classification from overallScore via Classification.fromScore")
    void shouldDeriveClassificationFromScore() {
        var result = AnalysisResult.score(
                1L,
                "squat-v1",
                new BigDecimal("0.82"),
                Map.of("depth", "good"),
                Map.of("knee_bottom_avg", 48.0)
        );

        assertThat(result.classification()).isEqualTo(Classification.GOOD);
    }

    @Test
    @DisplayName("Should have null id and scoredAt, letting persistence assign them")
    void shouldHaveNullIdAndScoredAt() {
        var result = AnalysisResult.score(
                1L, "squat-v1", new BigDecimal("0.5"), Map.of(), Map.of()
        );

        assertThat(result.id()).isNull();
        assertThat(result.scoredAt()).isNull();
    }

    @Test
    @DisplayName("Should carry through analysisId, modelVersion, overallScore, feedback and rawFeatures unchanged")
    void shouldCarryThroughFieldsUnchanged() {
        var feedback = Map.<String, Object>of("depth", "good");
        var rawFeatures = Map.<String, Object>of("knee_bottom_avg", 48.0);

        var result = AnalysisResult.score(42L, "squat-v1", new BigDecimal("0.9"), feedback, rawFeatures);

        assertThat(result.analysisId()).isEqualTo(42L);
        assertThat(result.modelVersion()).isEqualTo("squat-v1");
        assertThat(result.overallScore()).isEqualByComparingTo("0.9");
        assertThat(result.feedback()).isEqualTo(feedback);
        assertThat(result.rawFeatures()).isEqualTo(rawFeatures);
    }
}
