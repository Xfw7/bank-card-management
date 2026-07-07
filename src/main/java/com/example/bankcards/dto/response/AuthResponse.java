package com.example.bankcards.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UserResponse user
) {
}
