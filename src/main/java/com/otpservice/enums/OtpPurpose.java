package com.otpservice.enums;

/**
 * The purpose for which the OTP is being generated.
 *
 * This allows the system to differentiate between different OTP use cases,
 * enabling purpose-specific rate limits, expiry durations, or audit trails
 * in future enhancements.
 */
public enum OtpPurpose {

    /** OTP for user login / authentication. */
    LOGIN,

    /** OTP for new user registration. */
    REGISTRATION,

    /** OTP for password reset flow. */
    PASSWORD_RESET,

    /** OTP for transaction verification (e.g., payment confirmation). */
    TRANSACTION
}
