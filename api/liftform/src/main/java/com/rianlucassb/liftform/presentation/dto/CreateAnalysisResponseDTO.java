package com.rianlucassb.liftform.presentation.dto;

public record CreateAnalysisResponseDTO(
        Long analysisId,
        String uploadUrl,
        Long expiresIn
) {
}
