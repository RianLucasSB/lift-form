package com.rianlucassb.liftform.presentation.dto;

import java.util.List;

public record ErrorResponseDTO(
        List<String> errors
) {
}
