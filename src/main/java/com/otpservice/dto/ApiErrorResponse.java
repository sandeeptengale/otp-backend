package com.otpservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standardized error response DTO.
 *
 * Design Rationale:
 * A consistent error response format makes it easy for API clients to parse
 * and handle errors programmatically. Every error response has the same shape:
 * status code, error type, message, and timestamp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    /** HTTP status code (e.g., 400, 404, 429). */
    private int status;

    /** Error type/category (e.g., "BAD_REQUEST", "RATE_LIMIT_EXCEEDED"). */
    private String error;

    /** Human-readable error description. */
    private String message;

    /** Timestamp when the error occurred. */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
