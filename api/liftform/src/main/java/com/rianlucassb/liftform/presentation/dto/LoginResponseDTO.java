package com.rianlucassb.liftform.presentation.dto;

import lombok.Builder;

@Builder
public record LoginResponseDTO(
        String accessToken,
        String refreshToken
) {
}
