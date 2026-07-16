package com.rianlucassb.liftform.core.usecases.analysis.getanalysis;

import com.rianlucassb.liftform.core.domain.model.AnalysisResult;
import com.rianlucassb.liftform.core.domain.model.PipelineError;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;

import java.util.List;

public record GetAnalysisUseCaseOutput(
        VideoAnalysisStatus status,
        AnalysisResult result,
        List<PipelineError> errors
) {
}
