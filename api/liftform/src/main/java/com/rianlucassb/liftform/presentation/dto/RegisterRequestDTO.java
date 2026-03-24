package com.rianlucassb.liftform.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

public record RegisterRequestDTO(
        @NotEmpty(message = "Username must not be empty")
        @Min(value = 3, message = "Username must be at least 3 characters long")
        String username,

        @Email
        String email,

        @NotEmpty(message = "Password must not be empty")
        @Min(value = 6, message = "Password must be at least 6 characters")
        String password
) {
}
