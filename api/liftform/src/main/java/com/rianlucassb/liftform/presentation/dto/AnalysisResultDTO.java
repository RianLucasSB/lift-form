package com.rianlucassb.liftform.presentation.dto;

import com.rianlucassb.liftform.core.domain.model.enums.Classification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record AnalysisResultDTO(
        String modelVersion,
        BigDecimal overallScore,
        Classification classification,
        Map<String, Object> feedback,
        Instant scoredAt
) {
}
