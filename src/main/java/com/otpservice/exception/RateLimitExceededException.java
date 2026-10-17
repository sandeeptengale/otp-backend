package com.otpservice.exception;

/**
 * Thrown when a rate limit is exceeded (either generation or verification).
 * Maps to HTTP 429 Too Many Requests.
 */
public class RateLimitExceededException extends OtpException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
