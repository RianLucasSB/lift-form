package com.rianlucassb.liftform.core.usecases.analysis.recordresult;

import java.math.BigDecimal;
import java.util.Map;

public record RecordAnalysisResultUseCaseInput(
        Long videoAnalysisId,
        String modelVersion,
        BigDecimal overallScore,
        Map<String, Object> feedback,
        Map<String, Object> rawFeatures
) {
}
