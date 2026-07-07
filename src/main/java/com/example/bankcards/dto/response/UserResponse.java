package com.example.bankcards.dto.response;

import com.example.bankcards.entity.enums.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        Role role,
        boolean enabled,
        Instant createdAt
) {
}
