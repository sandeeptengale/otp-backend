package com.otpservice.exception;

import com.otpservice.dto.ApiErrorResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler that translates exceptions into consistent API error responses.
 *
 * Design Pattern (Template Method / Centralized Error Handling):
 * Instead of each controller method handling its own errors with try-catch blocks,
 * @RestControllerAdvice intercepts all exceptions globally and formats them into
 * a standardized ApiErrorResponse. This ensures:
 *
 *   1. Consistent error response format across all endpoints
 *   2. No exception details leaked to the client (security)
 *   3. Centralized logging of all errors
 *   4. Clean controller methods that only handle the happy path
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle business logic OTP errors → 400 Bad Request.
     */
    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpException(OtpException ex) {
        log.warn("OTP error: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(
                ApiErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("OTP_ERROR")
                        .message(ex.getMessage())
                        .build()
        );
    }

    /**
     * Handle OTP not found → 404 Not Found.
     */
    @ExceptionHandler(OtpNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(OtpNotFoundException ex) {
        log.warn("OTP not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiErrorResponse.builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .error("NOT_FOUND")
                        .message(ex.getMessage())
                        .build()
        );
    }

    /**
     * Handle rate limit exceeded → 429 Too Many Requests.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                ApiErrorResponse.builder()
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .error("RATE_LIMIT_EXCEEDED")
                        .message(ex.getMessage())
                        .build()
        );
    }

    /**
     * Handle Bean Validation errors (@NotBlank, @Pattern, etc.) → 400 Bad Request.
     *
     * Collects all field-level validation errors into a single comma-separated message,
     * so the client knows exactly which fields failed and why.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation error: {}", errors);
        return ResponseEntity.badRequest().body(
                ApiErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("VALIDATION_ERROR")
                        .message(errors)
                        .build()
        );
    }

    /**
     * Catch-all for unexpected errors → 500 Internal Server Error.
     *
     * SECURITY: Never expose internal exception details (stack traces, class names)
     * to the client. Log the full error server-side for debugging.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiErrorResponse.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error("INTERNAL_ERROR")
                        .message("An unexpected error occurred. Please try again later.")
                        .build()
        );
    }
}
