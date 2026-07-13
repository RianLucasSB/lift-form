package com.rianlucassb.liftform.core.domain.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificationTest {

    @Test
    @DisplayName("Should classify a score of 0.75 as GOOD")
    void shouldClassifyHighScoreAsGood() {
        assertThat(Classification.fromScore(new BigDecimal("0.75"))).isEqualTo(Classification.GOOD);
    }

    @Test
    @DisplayName("Should classify a score of exactly 0.7 as GOOD (inclusive lower bound)")
    void shouldClassifyExactlyPointSevenAsGood() {
        assertThat(Classification.fromScore(new BigDecimal("0.7"))).isEqualTo(Classification.GOOD);
    }

    @Test
    @DisplayName("Should classify a score of 0.55 as FAIR")
    void shouldClassifyMidScoreAsFair() {
        assertThat(Classification.fromScore(new BigDecimal("0.55"))).isEqualTo(Classification.FAIR);
    }

    @Test
    @DisplayName("Should classify a score of exactly 0.5 as FAIR (inclusive lower bound)")
    void shouldClassifyExactlyPointFiveAsFair() {
        assertThat(Classification.fromScore(new BigDecimal("0.5"))).isEqualTo(Classification.FAIR);
    }

    @Test
    @DisplayName("Should classify a score just below 0.7 as FAIR, not GOOD")
    void shouldClassifyJustBelowPointSevenAsFair() {
        assertThat(Classification.fromScore(new BigDecimal("0.6999"))).isEqualTo(Classification.FAIR);
    }

    @Test
    @DisplayName("Should classify a score of 0.2 as POOR")
    void shouldClassifyLowScoreAsPoor() {
        assertThat(Classification.fromScore(new BigDecimal("0.2"))).isEqualTo(Classification.POOR);
    }

    @Test
    @DisplayName("Should classify a score just below 0.5 as POOR, not FAIR")
    void shouldClassifyJustBelowPointFiveAsPoor() {
        assertThat(Classification.fromScore(new BigDecimal("0.4999"))).isEqualTo(Classification.POOR);
    }

    @Test
    @DisplayName("Should classify a score of 0.0 as POOR")
    void shouldClassifyZeroAsPoor() {
        assertThat(Classification.fromScore(BigDecimal.ZERO)).isEqualTo(Classification.POOR);
    }

    @Test
    @DisplayName("Should classify a score of 1.0 as GOOD")
    void shouldClassifyOneAsGood() {
        assertThat(Classification.fromScore(BigDecimal.ONE)).isEqualTo(Classification.GOOD);
    }
}
