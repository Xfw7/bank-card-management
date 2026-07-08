package com.example.bankcards.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "Validation failed"),
    MALFORMED_JSON("MALFORMED_JSON", HttpStatus.BAD_REQUEST, "Malformed JSON request"),
    BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST, "Bad request"),
    DATA_INTEGRITY_VIOLATION("DATA_INTEGRITY_VIOLATION", HttpStatus.CONFLICT, "Data integrity violation"),
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Authentication required"),
    BAD_CREDENTIALS("BAD_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid username or password"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "JWT token has expired"),
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN, "Access denied"),
    NOT_FOUND("NOT_FOUND", HttpStatus.NOT_FOUND, "Resource not found"),
    CONFLICT("CONFLICT", HttpStatus.CONFLICT, "Conflict"),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
   
    INSUFFICIENT_BALANCE("INSUFFICIENT_BALANCE", HttpStatus.CONFLICT, "Insufficient balance"), 
    CARD_BLOCKED("CARD_BLOCKED", HttpStatus.CONFLICT, "Card is blocked"),
    CARD_EXPIRED("CARD_EXPIRED", HttpStatus.CONFLICT, "Card is expired"),
    CARD_DELETED("CARD_DELETED", HttpStatus.CONFLICT, "Card is deleted"),
    SAME_CARD_TRANSFER("SAME_CARD_TRANSFER", HttpStatus.BAD_REQUEST, "Source and destination cards must differ"),
    USER_DISABLED("USER_DISABLED", HttpStatus.FORBIDDEN, "User is disabled");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
