package com.rianlucassb.liftform.application.dto;

import lombok.Builder;

@Builder
public record LoginResponseDTO(
        String accessToken,
        String refreshToken
) {
}
