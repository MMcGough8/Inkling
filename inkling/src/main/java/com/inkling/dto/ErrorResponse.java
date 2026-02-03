package com.inkling.dto;

import java.time.Instant;

/**
 * Standard error response format for all API errors.
 */
public record ErrorResponse(
        String error,
        String message,
        int status,
        Instant timestamp
) {
    public static ErrorResponse of(String error, String message, int status) {
        return new ErrorResponse(error, message, status, Instant.now());
    }
}
