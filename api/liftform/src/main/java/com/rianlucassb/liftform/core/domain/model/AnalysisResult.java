package com.rianlucassb.liftform.core.domain.model;

import com.rianlucassb.liftform.core.domain.model.enums.Classification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record AnalysisResult(
    Long id,
    Long analysisId,
    String modelVersion,
    BigDecimal overallScore,
    Classification classification,
    Map<String, Object> feedback,
    Map<String, Object> rawFeatures,
    Instant scoredAt
) {}