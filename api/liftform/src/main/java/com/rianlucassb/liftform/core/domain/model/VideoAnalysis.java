package com.rianlucassb.liftform.core.domain.model;

import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;

import java.time.Instant;
import java.util.UUID;

public record VideoAnalysis(
    Long id,
    UUID userId,
    ExerciseType exerciseType,
    String videoS3Key,
    VideoAnalysisStatus status,
    Instant createdAt,
    Instant updatedAt
) {}