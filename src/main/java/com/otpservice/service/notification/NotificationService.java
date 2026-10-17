package com.otpservice.service.notification;

/**
 * Strategy interface for OTP delivery.
 *
 * ═══════════════════════════════════════════════════════════════
 * DESIGN DEEP DIVE: Strategy Pattern
 * ═══════════════════════════════════════════════════════════════
 *
 * The Strategy Pattern defines a family of algorithms (notification channels),
 * encapsulates each one, and makes them interchangeable. The service layer
 * doesn't know (or care) whether the OTP is being sent via SMS, email, or
 * printed to the console.
 *
 * Benefits:
 *   1. Open/Closed Principle: Add new channels (WhatsApp, Push) without
 *      modifying existing code — just create a new implementation.
 *   2. Testability: Inject a mock/console implementation in tests.
 *   3. Runtime flexibility: Select implementation based on the request.
 *
 * Implementations:
 *   - ConsoleNotificationService: Dev/test (prints to log)
 *   - SmsNotificationService: Production (calls SMS gateway — stubbed)
 */
public interface NotificationService {

    /**
     * Send an OTP to the specified identifier via this channel.
     *
     * @param identifier the target (phone number, email, etc.)
     * @param otp the plain-text OTP to deliver
     * @return true if delivery was successful
     */
    boolean sendOtp(String identifier, String otp);

    /**
     * Get the channel type this service handles.
     * Used for runtime channel selection.
     */
    String getChannel();
}
