package com.example.bankcards.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        Long id,
        Long fromCardId,
        Long toCardId,
        BigDecimal amount,
        Instant createdAt
) {
}
