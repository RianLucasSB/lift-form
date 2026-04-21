package com.rianlucassb.liftform.presentation.dto;

import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAnalysisRequestDTO(
        @NotBlank(message = "File must be not blank")
        String fileName,
        @NotNull(message = "Exercise must be not null")
        ExerciseType exerciseType
) {
}
