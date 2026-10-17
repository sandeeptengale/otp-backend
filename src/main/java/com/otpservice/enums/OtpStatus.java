package com.otpservice.enums;

/**
 * Represents the lifecycle state of an OTP record.
 *
 * State transitions (one-way — terminal states cannot go back to ACTIVE):
 *
 *   ACTIVE  →  VERIFIED   (correct OTP provided)
 *   ACTIVE  →  EXPIRED    (TTL exceeded)
 *   ACTIVE  →  BLOCKED    (max verification attempts exceeded)
 *
 * Why is this important?
 * Once an OTP leaves the ACTIVE state, it can NEVER be used for verification again.
 * This ensures single-use semantics — a critical security property.
 */
public enum OtpStatus {

    /** OTP has been generated and is awaiting verification. */
    ACTIVE,

    /** OTP was successfully verified. Terminal state. */
    VERIFIED,

    /** OTP validity period has elapsed. Terminal state. */
    EXPIRED,

    /** Too many failed verification attempts. Terminal state. */
    BLOCKED
}
