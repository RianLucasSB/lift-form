package com.rianlucassb.liftform.core.usecases.analysis.create;

import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;

public record CreateAnalysisUseCaseInput(
        String userId,
        ExerciseType exerciseType,
        String fileName
) {
}
