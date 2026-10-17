package com.otpservice.service;

import com.otpservice.config.OtpProperties;
import com.otpservice.exception.RateLimitExceededException;
import com.otpservice.repository.OtpRecordRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service responsible for enforcing rate limits on OTP operations.
 *
 * ═══════════════════════════════════════════════════════════════
 * DESIGN DEEP DIVE: Why Rate Limiting?
 * ═══════════════════════════════════════════════════════════════
 *
 * Without rate limiting, an attacker could:
 *
 * 1. GENERATION FLOODING (SMS Pumping):
 *    Send thousands of OTP requests for the same phone number,
 *    causing massive SMS charges and denial of service.
 *    → Mitigation: Max N OTPs per identifier per time window.
 *
 * 2. BRUTE FORCE VERIFICATION:
 *    A 6-digit OTP has 1,000,000 combinations. At 1000 requests/second,
 *    an attacker can try all combinations in ~17 minutes.
 *    → Mitigation: Max attempts per OTP (handled in OtpService),
 *      combined with per-identifier rate limits.
 *
 * ═══════════════════════════════════════════════════════════════
 * CURRENT IMPLEMENTATION: Database-Based Rate Limiting
 * ═══════════════════════════════════════════════════════════════
 *
 * For Phase 1, we count recent OTP records in the database.
 * This works well for moderate traffic and is simple to understand.
 *
 * For production at scale, consider:
 *   - Redis-based sliding window counters (atomic, distributed)
 *   - Bucket4j library (token bucket algorithm, in-memory)
 *   - API Gateway rate limiting (Kong, AWS API Gateway)
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final OtpRecordRepository otpRecordRepository;
    private final OtpProperties otpProperties;

    public RateLimiterService(OtpRecordRepository otpRecordRepository,
                              OtpProperties otpProperties) {
        this.otpRecordRepository = otpRecordRepository;
        this.otpProperties = otpProperties;
    }

    /**
     * Check if OTP generation is allowed for the given identifier.
     *
     * Counts how many OTPs were generated for this identifier within
     * the configured time window. If the count exceeds the limit,
     * throws RateLimitExceededException (→ HTTP 429).
     *
     * Example with default config:
     *   Window = 600 seconds (10 minutes)
     *   Max = 3 OTPs per window
     *
     *   If user requested 3 OTPs in the last 10 minutes → BLOCKED
     *   If user requested 2 OTPs in the last 10 minutes → ALLOWED
     *
     * @param identifier the phone number or email
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    public void checkGenerationRateLimit(String identifier) {
        LocalDateTime windowStart = LocalDateTime.now()
                .minusSeconds(otpProperties.getGenerationWindowSeconds());

        long recentCount = otpRecordRepository
                .countByIdentifierAndCreatedAtAfter(identifier, windowStart);

        log.debug("Rate limit check for '{}': {} OTPs in last {} seconds (max: {})",
                identifier, recentCount,
                otpProperties.getGenerationWindowSeconds(),
                otpProperties.getMaxGenerationPerWindow());

        if (recentCount >= otpProperties.getMaxGenerationPerWindow()) {
            throw new RateLimitExceededException(
                    String.format("OTP generation rate limit exceeded. Maximum %d OTPs allowed per %d seconds.",
                            otpProperties.getMaxGenerationPerWindow(),
                            otpProperties.getGenerationWindowSeconds())
            );
        }
    }
}
