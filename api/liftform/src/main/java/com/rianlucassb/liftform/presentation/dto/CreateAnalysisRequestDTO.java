package com.rianlucassb.liftform.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAnalysisRequestDTO(
        @NotBlank(message = "File must be not blank")
        String fileName,
        @NotBlank(message = "Exercise must be not blank")
        String exerciseType
) {
}
