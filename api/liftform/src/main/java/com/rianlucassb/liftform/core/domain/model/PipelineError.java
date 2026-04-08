package com.rianlucassb.liftform.core.domain.model;

import com.rianlucassb.liftform.core.domain.model.enums.PipelineStage;

import java.time.Instant;

public record PipelineError(
    Long id,
    Long analysisId,
    PipelineStage stage,
    String errorMessage,
    String stackTrace,
    Instant occurredAt
) {}