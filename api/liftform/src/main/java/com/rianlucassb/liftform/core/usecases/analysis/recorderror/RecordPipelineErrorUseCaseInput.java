package com.rianlucassb.liftform.core.usecases.analysis.recorderror;

import com.rianlucassb.liftform.core.domain.model.enums.PipelineStage;

public record RecordPipelineErrorUseCaseInput(
        Long analysisId,
        PipelineStage stage,
        String errorMessage,
        String stackTrace
) {
}
