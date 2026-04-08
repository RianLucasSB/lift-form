package com.rianlucassb.liftform.core.domain.model;

import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;

import java.time.Instant;

public record VideoAnalysis(
    Long id,
    Long userId,
    String exerciseType,
    String videoS3Key,
    VideoAnalysisStatus status,
    Instant createdAt,
    Instant updatedAt
) {}