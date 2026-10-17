package com.otpservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Service responsible for OTP generation and hash operations.
 *
 * ═══════════════════════════════════════════════════════════════
 * SECURITY DEEP DIVE: Why We Hash OTPs
 * ═══════════════════════════════════════════════════════════════
 *
 * Problem: If we store "482910" in plain text and the database is breached,
 * an attacker instantly has all active OTPs and can impersonate any user.
 *
 * Solution: Store BCrypt("482910") = "$2a$10$xJ8k..." instead.
 * BCrypt is a one-way hash — you cannot reverse it to get "482910".
 * During verification, we hash the user's input and compare hashes.
 *
 * Why BCrypt specifically?
 *   - It includes a random SALT, so identical OTPs produce different hashes
 *   - It has a configurable cost factor (work factor) to slow down brute-force
 *   - It's the industry standard for password/secret hashing
 *
 * ═══════════════════════════════════════════════════════════════
 * SECURITY DEEP DIVE: Why SecureRandom, Not Math.random()?
 * ═══════════════════════════════════════════════════════════════
 *
 * Math.random() uses a PRNG (Pseudo-Random Number Generator) that is:
 *   - Seeded with a predictable value (system time)
 *   - Deterministic: given the seed, all future values can be predicted
 *   - NOT suitable for security-sensitive operations
 *
 * SecureRandom uses the OS's cryptographic random source (/dev/urandom on Linux,
 * CryptGenRandom on Windows), which is:
 *   - Seeded from hardware entropy (mouse movements, disk I/O timing, etc.)
 *   - Cryptographically secure: output is indistinguishable from true randomness
 *   - Required for all security tokens, OTPs, session IDs, etc.
 */
@Service
public class OtpHashingService {

    private static final Logger log = LoggerFactory.getLogger(OtpHashingService.class);

    /**
     * BCrypt encoder with strength 10 (2^10 = 1024 hashing iterations).
     * Higher strength = slower hashing = harder to brute-force, but also
     * slower to verify. 10 is a good balance for OTPs.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Cryptographically secure random number generator.
     * Thread-safe and suitable for concurrent OTP generation.
     */
    private final SecureRandom secureRandom;

    public OtpHashingService() {
        this.passwordEncoder = new BCryptPasswordEncoder(10);
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generate a cryptographically secure numeric OTP of the specified length.
     *
     * @param length number of digits (e.g., 6 → generates "048271")
     * @return the plain-text OTP (will be hashed before storage)
     *
     * Implementation Note:
     * We use modular arithmetic (% 10) on SecureRandom output to get each digit.
     * StringBuilder ensures efficient string construction for the configured length.
     */
    public String generateOtp(int length) {
        StringBuilder otp = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            otp.append(secureRandom.nextInt(10)); // 0-9
        }
        String generatedOtp = otp.toString();
        log.debug("Generated OTP of length {}", length);
        return generatedOtp;
    }

    /**
     * Hash an OTP using BCrypt.
     * The hash includes a random salt, so the same OTP produces different hashes each time.
     *
     * @param rawOtp the plain-text OTP
     * @return BCrypt hash string (e.g., "$2a$10$xJ8k...")
     */
    public String hashOtp(String rawOtp) {
        return passwordEncoder.encode(rawOtp);
    }

    /**
     * Verify a plain-text OTP against a stored BCrypt hash.
     *
     * BCrypt handles salt extraction and comparison internally.
     * This comparison is timing-safe (constant time) to prevent timing attacks.
     *
     * What is a timing attack?
     * If string comparison returns as soon as a mismatch is found,
     * an attacker can measure response times to determine how many
     * characters matched. BCrypt's matches() always takes the same time.
     *
     * @param rawOtp the OTP entered by the user
     * @param hashedOtp the BCrypt hash stored in the database
     * @return true if they match
     */
    public boolean verifyOtp(String rawOtp, String hashedOtp) {
        return passwordEncoder.matches(rawOtp, hashedOtp);
    }
}
