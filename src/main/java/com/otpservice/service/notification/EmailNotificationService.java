package com.otpservice.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Stub Email notification service.
 *
 * In production, this would use:
 *   - Spring Boot Starter Mail (JavaMailSender)
 *   - AWS SES (Simple Email Service)
 *   - SendGrid
 */
@Service
public class EmailNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    @Override
    public boolean sendOtp(String identifier, String otp) {
        // TODO: Replace with actual email sending logic
        // Example with Spring Mail:
        //   SimpleMailMessage message = new SimpleMailMessage();
        //   message.setTo(identifier);
        //   message.setSubject("Your OTP Code");
        //   message.setText("Your OTP is: " + otp + ". Valid for 5 minutes.");
        //   mailSender.send(message);

        log.info("[EMAIL STUB] Would send OTP '{}' to '{}'", otp, identifier);
        return true;
    }

    @Override
    public String getChannel() {
        return "EMAIL";
    }
}
