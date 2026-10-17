package com.otpservice.exception;

/**
 * Thrown when an OTP record is not found by the provided reference ID.
 * Maps to HTTP 404 Not Found.
 *
 * Security Note:
 * The error message should be generic ("Invalid reference ID") rather than
 * revealing whether the ID ever existed. This prevents information leakage
 * that could aid enumeration attacks.
 */
public class OtpNotFoundException extends OtpException {

    public OtpNotFoundException(String message) {
        super(message);
    }
}
