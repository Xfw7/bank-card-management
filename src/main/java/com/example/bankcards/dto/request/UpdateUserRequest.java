package com.example.bankcards.dto.request;

import com.example.bankcards.entity.enums.Role;

public record UpdateUserRequest(
        Boolean enabled,
        Role role
) {
}
