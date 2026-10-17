package com.otpservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Externalized OTP configuration bound to the "otp" prefix in application.yml.
 *
 * Design Rationale:
 * Using @ConfigurationProperties instead of @Value allows type-safe, validated,
 * and grouped configuration. This makes it easy to change OTP behavior (length,
 * expiry, rate limits) without modifying any Java code — just update application.yml.
 *
 * Example in application.yml:
 *   otp:
 *     length: 6
 *     expiry-seconds: 300
 *     max-attempts: 5
 */
@Configuration
@ConfigurationProperties(prefix = "otp")
@Validated
@Getter
@Setter
public class OtpProperties {

    /**
     * Number of digits in the generated OTP.
     * Range: 4-8 digits. Industry standard is 6.
     * A 6-digit OTP has 1,000,000 possible combinations.
     */
    @Min(4)
    @Max(8)
    @NotNull
    private Integer length = 6;

    /**
     * How long (in seconds) an OTP remains valid after generation.
     * Default: 300 seconds (5 minutes).
     * Too short → poor user experience. Too long → security risk.
     */
    @Min(60)
    @Max(900)
    @NotNull
    private Integer expirySeconds = 300;

    /**
     * Maximum number of incorrect verification attempts before the OTP is blocked.
     * After this limit, the OTP status transitions to BLOCKED.
     * Default: 5 attempts.
     */
    @Min(3)
    @Max(10)
    @NotNull
    private Integer maxAttempts = 5;

    /**
     * Maximum number of OTP generation requests allowed per identifier
     * within the rate-limit window.
     * This prevents OTP flooding / SMS pumping attacks.
     */
    @Min(1)
    @Max(10)
    @NotNull
    private Integer maxGenerationPerWindow = 3;

    /**
     * Rate-limit window duration in seconds for generation requests.
     * Default: 600 seconds (10 minutes).
     */
    @Min(60)
    @Max(3600)
    @NotNull
    private Integer generationWindowSeconds = 600;

    /**
     * Cron expression for the OTP cleanup scheduler.
     * Default: runs every 5 minutes.
     */
    @NotNull
    private String cleanupCron = "0 */5 * * * *";
}
