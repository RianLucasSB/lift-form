package com.rianlucassb.liftform.presentation.dto;

import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;

import java.util.List;

public record GetAnalysisResponseDTO(
        VideoAnalysisStatus status,
        AnalysisResultDTO result,
        List<String> errors
) {
}
