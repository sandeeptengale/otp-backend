package com.otpservice.enums;

/**
 * Supported channels for OTP delivery.
 *
 * Design Note (Strategy Pattern):
 * Each channel maps to a different NotificationService implementation.
 * The service layer selects the correct implementation at runtime based
 * on the channel specified in the request.
 *
 * For local development, use CONSOLE — it prints the OTP to the application log.
 */
public enum ChannelType {

    /** Deliver OTP via SMS. Requires an SMS gateway integration in production. */
    SMS,

    /** Deliver OTP via email. Requires SMTP configuration in production. */
    EMAIL,

    /** Log OTP to console. Used for local development and testing. */
    CONSOLE
}
