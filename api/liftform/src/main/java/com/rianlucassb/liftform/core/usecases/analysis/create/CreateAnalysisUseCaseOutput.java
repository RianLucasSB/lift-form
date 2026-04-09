package com.rianlucassb.liftform.core.usecases.analysis.create;

public record CreateAnalysisUseCaseOutput(
        Long analysisId,
        String uploadUrl,
        int expiresIn
) {
}
