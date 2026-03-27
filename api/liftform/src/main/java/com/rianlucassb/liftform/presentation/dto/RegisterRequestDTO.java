package com.rianlucassb.liftform.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
        @NotEmpty(message = "Username must not be empty")
        @Email
        String email,

        @NotEmpty
        String username,

        @NotEmpty(message = "Password must not be empty")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password
) {
}
