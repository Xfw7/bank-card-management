package com.example.bankcards.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        List<ValidationError> errors
) {

    public record ValidationError(String field, String message) {
    }

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(Instant.now(), status, code, message, null);
    }

    public static ErrorResponse validation(int status, String code, String message, List<ValidationError> errors) {
        return new ErrorResponse(Instant.now(), status, code, message, errors);
    }
}
