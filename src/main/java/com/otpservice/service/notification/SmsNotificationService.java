package com.otpservice.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Stub SMS notification service.
 *
 * In production, this would integrate with an SMS gateway like:
 *   - Twilio (https://www.twilio.com)
 *   - AWS SNS (Simple Notification Service)
 *   - MSG91
 *   - Vonage (Nexmo)
 *
 * For now, it simulates SMS delivery by logging the OTP.
 * Replace the sendOtp() body with actual SMS API calls when ready.
 */
@Service
public class SmsNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    @Override
    public boolean sendOtp(String identifier, String otp) {
        // TODO: Replace with actual SMS gateway integration
        // Example with Twilio:
        //   Message message = Message.creator(
        //       new PhoneNumber(identifier),
        //       new PhoneNumber(twilioFromNumber),
        //       "Your OTP is: " + otp
        //   ).create();

        log.info("[SMS STUB] Would send OTP '{}' to '{}'", otp, identifier);
        return true;
    }

    @Override
    public String getChannel() {
        return "SMS";
    }
}
