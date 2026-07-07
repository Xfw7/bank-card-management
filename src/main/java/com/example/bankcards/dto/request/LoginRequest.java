package com.example.bankcards.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Username is required")
        @Size(max = 50)
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}
