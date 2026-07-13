package com.rianlucassb.liftform.infraestructure.adapter.messaging.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AnalysisFinishedMessage(
        Long videoAnalysisId,
        String modelVersion,
        BigDecimal overallScore,
        Map<String, Object> feedback,
        Map<String, Object> rawFeatures
) {
}
