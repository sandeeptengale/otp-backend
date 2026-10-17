package com.otpservice.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Console-based OTP notification service for local development.
 *
 * Instead of sending a real SMS or email, this implementation prints the OTP
 * to the application log. This allows you to:
 *   - Test the full OTP flow without any external dependencies
 *   - See the OTP in the console output to manually verify
 *   - Run the application completely offline
 *
 * The log output is clearly marked with a banner for easy spotting:
 *
 *   ╔══════════════════════════════════════╗
 *   ║  OTP for +919876543210: 482910       ║
 *   ╚══════════════════════════════════════╝
 */
@Service
public class ConsoleNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleNotificationService.class);

    @Override
    public boolean sendOtp(String identifier, String otp) {
        log.info("\n" +
                "╔══════════════════════════════════════════════════════╗\n" +
                "║                    OTP DELIVERY                     ║\n" +
                "╠══════════════════════════════════════════════════════╣\n" +
                "║  Identifier : {}\n" +
                "║  OTP        : {}\n" +
                "║  Channel    : CONSOLE\n" +
                "╚══════════════════════════════════════════════════════╝",
                identifier, otp);
        return true;
    }

    @Override
    public String getChannel() {
        return "CONSOLE";
    }
}
