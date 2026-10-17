package com.otpservice.exception;

/**
 * Base exception for all OTP-related business logic errors.
 *
 * Design Note:
 * Custom exceptions provide meaningful error types that the GlobalExceptionHandler
 * can map to appropriate HTTP status codes. This is cleaner than throwing generic
 * RuntimeExceptions and trying to parse error messages.
 */
public class OtpException extends RuntimeException {

    public OtpException(String message) {
        super(message);
    }

    public OtpException(String message, Throwable cause) {
        super(message, cause);
    }
}
