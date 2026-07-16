package com.rianlucassb.liftform.core.usecases.analysis.listanalyses;

import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;

import java.time.Instant;

public record AnalysisSummary(
        Long id,
        ExerciseType exerciseType,
        VideoAnalysisStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
