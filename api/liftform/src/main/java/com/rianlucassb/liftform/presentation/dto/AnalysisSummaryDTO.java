package com.rianlucassb.liftform.presentation.dto;

import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;

import java.time.Instant;

public record AnalysisSummaryDTO(
        Long id,
        ExerciseType exerciseType,
        VideoAnalysisStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
