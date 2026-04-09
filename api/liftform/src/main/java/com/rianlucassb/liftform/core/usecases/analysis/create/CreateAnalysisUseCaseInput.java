package com.rianlucassb.liftform.core.usecases.analysis.create;

public record CreateAnalysisUseCaseInput(
        String userId,
        String exerciseType,
        String fileName
) {
}
