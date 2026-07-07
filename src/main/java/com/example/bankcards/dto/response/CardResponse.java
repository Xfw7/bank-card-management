package com.example.bankcards.dto.response;

import com.example.bankcards.entity.enums.CardStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CardResponse(
        Long id,
        String maskedNumber,
        String owner,
        LocalDate expiryDate,
        CardStatus status,
        BigDecimal balance,
        boolean blockRequested,
        Instant createdAt
) {
}
