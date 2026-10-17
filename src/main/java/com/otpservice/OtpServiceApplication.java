package com.otpservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the OTP Verification Service.
 *
 * @EnableScheduling enables the OTP cleanup scheduler to purge expired records.
 */
@SpringBootApplication
@EnableScheduling
public class OtpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OtpServiceApplication.class, args);
    }
}
