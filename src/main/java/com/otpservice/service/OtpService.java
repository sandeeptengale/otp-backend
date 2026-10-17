package com.otpservice.service;

import com.otpservice.config.OtpProperties;
import com.otpservice.dto.OtpGenerateRequest;
import com.otpservice.dto.OtpGenerateResponse;
import com.otpservice.dto.OtpVerifyRequest;
import com.otpservice.dto.OtpVerifyResponse;
import com.otpservice.entity.OtpRecord;
import com.otpservice.enums.OtpStatus;
import com.otpservice.exception.OtpException;
import com.otpservice.exception.OtpNotFoundException;
import com.otpservice.repository.OtpRecordRepository;
import com.otpservice.service.notification.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Core OTP business logic service.
 *
 * This is the orchestrator that coordinates all OTP operations:
 *   1. Rate limiting checks
 *   2. OTP generation and hashing
 *   3. Previous OTP invalidation
 *   4. Database persistence
 *   5. Notification delivery
 *   6. Verification with all security checks
 *
 * ═══════════════════════════════════════════════════════════════
 * DESIGN DEEP DIVE: Transactional Integrity
 * ═══════════════════════════════════════════════════════════════
 *
 * Both generateOtp() and verifyOtp() are annotated with @Transactional.
 * This ensures that if ANY step fails (e.g., database write, notification),
 * ALL changes are rolled back. Without this:
 *   - An OTP could be saved to DB but never delivered to the user
 *   - An OTP could be marked verified but the response fails
 *
 * @Transactional guarantees atomicity: either everything succeeds, or nothing changes.
 *
 * ═══════════════════════════════════════════════════════════════
 * DESIGN DEEP DIVE: Channel Selection via Strategy Map
 * ═══════════════════════════════════════════════════════════════
 *
 * Spring injects ALL beans implementing NotificationService into a List.
 * We convert this list into a Map<channelName, service> for O(1) lookup.
 * This avoids if-else chains and makes adding new channels trivial.
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final OtpRecordRepository otpRecordRepository;
    private final OtpHashingService otpHashingService;
    private final RateLimiterService rateLimiterService;
    private final OtpProperties otpProperties;

    /**
     * Map of channel name → NotificationService implementation.
     * Populated from all Spring beans implementing NotificationService.
     *
     * Example contents:
     *   "CONSOLE" → ConsoleNotificationService
     *   "SMS"     → SmsNotificationService
     *   "EMAIL"   → EmailNotificationService
     */
    private final Map<String, NotificationService> notificationServiceMap;

    /**
     * Constructor injection (preferred over @Autowired field injection).
     *
     * Why constructor injection?
     *   1. Immutability: fields can be 'final'
     *   2. Required dependencies are explicit
     *   3. Easier to test (pass mocks via constructor)
     *   4. Fails fast at startup if a dependency is missing
     */
    public OtpService(OtpRecordRepository otpRecordRepository,
                      OtpHashingService otpHashingService,
                      RateLimiterService rateLimiterService,
                      OtpProperties otpProperties,
                      List<NotificationService> notificationServices) {
        this.otpRecordRepository = otpRecordRepository;
        this.otpHashingService = otpHashingService;
        this.rateLimiterService = rateLimiterService;
        this.otpProperties = otpProperties;

        // Convert List<NotificationService> → Map<channel, service> for O(1) lookup
        this.notificationServiceMap = notificationServices.stream()
                .collect(Collectors.toMap(
                        NotificationService::getChannel,
                        Function.identity()
                ));

        log.info("OtpService initialized with notification channels: {}",
                notificationServiceMap.keySet());
    }

    /**
     * Generate a new OTP for the given identifier and deliver it via the specified channel.
     *
     * Flow:
     *   1. Check generation rate limit for the identifier
     *   2. Invalidate any existing ACTIVE OTPs for this identifier (single active OTP policy)
     *   3. Generate a cryptographically secure random OTP
     *   4. Hash the OTP with BCrypt before storing
     *   5. Save the OTP record to the database
     *   6. Deliver the plain-text OTP via the notification channel
     *   7. Return the reference ID (NOT the OTP) to the caller
     *
     * @param request contains identifier, channel, and purpose
     * @return response with referenceId and expiry info
     */
    @Transactional
    public OtpGenerateResponse generateOtp(OtpGenerateRequest request) {
        String identifier = request.getIdentifier().trim();
        log.info("OTP generation requested for identifier: {}, channel: {}, purpose: {}",
                maskIdentifier(identifier), request.getChannel(), request.getPurpose());

        // Step 1: Rate limit check
        rateLimiterService.checkGenerationRateLimit(identifier);

        // Step 2: Invalidate existing ACTIVE OTPs for this identifier
        // This implements the "invalidate-and-regenerate" policy.
        // Why? If a user requests a new OTP, the old one should no longer work.
        // This prevents confusion about which OTP is valid.
        invalidateExistingOtps(identifier);

        // Step 3: Generate a secure random OTP
        String rawOtp = otpHashingService.generateOtp(otpProperties.getLength());

        // Step 4: Hash the OTP before storage (NEVER store plain text)
        String otpHash = otpHashingService.hashOtp(rawOtp);

        // Step 5: Create and persist the OTP record
        OtpRecord record = OtpRecord.create(
                identifier,
                otpHash,
                request.getChannel(),
                request.getPurpose(),
                otpProperties.getExpirySeconds()
        );
        otpRecordRepository.save(record);
        log.info("OTP record created with referenceId: {}", record.getReferenceId());

        // Step 6: Deliver the OTP via the selected channel
        NotificationService notifier = getNotificationService(request.getChannel().name());
        boolean delivered = notifier.sendOtp(identifier, rawOtp);

        if (!delivered) {
            // If delivery fails, we could either:
            // a) Throw an exception and roll back (strict approach)
            // b) Log a warning and let the user retry (lenient approach)
            // We choose (a) for enterprise reliability
            throw new OtpException("Failed to deliver OTP. Please try again.");
        }

        // Step 7: Return reference ID (the OTP itself is NEVER returned via API)
        return OtpGenerateResponse.builder()
                .referenceId(record.getReferenceId())
                .expiresInSeconds(otpProperties.getExpirySeconds())
                .message("OTP sent successfully via " + request.getChannel())
                .build();
    }

    /**
     * Verify an OTP against the stored hash.
     *
     * Flow:
     *   1. Look up the OTP record by reference ID
     *   2. Check if the OTP is still ACTIVE (not already verified/expired/blocked)
     *   3. Check if the OTP has expired (time-based check)
     *   4. Check if max verification attempts have been exceeded
     *   5. Compare the submitted OTP hash with the stored hash
     *   6. Update status based on result (VERIFIED or increment attempts)
     *
     * @param request contains referenceId and the user-entered OTP
     * @return response indicating success/failure with remaining attempts
     */
    @Transactional
    public OtpVerifyResponse verifyOtp(OtpVerifyRequest request) {
        log.info("OTP verification requested for referenceId: {}", request.getReferenceId());

        // Step 1: Find the OTP record
        OtpRecord record = otpRecordRepository
                .findByReferenceId(request.getReferenceId())
                .orElseThrow(() -> new OtpNotFoundException(
                        "Invalid reference ID. No OTP found for the provided reference."));

        // Step 2: Check if OTP is in ACTIVE state
        if (record.getStatus() != OtpStatus.ACTIVE) {
            log.warn("Verification attempted on non-ACTIVE OTP. Status: {}, Ref: {}",
                    record.getStatus(), request.getReferenceId());
            return OtpVerifyResponse.builder()
                    .verified(false)
                    .message("This OTP is no longer active. Status: " + record.getStatus())
                    .build();
        }

        // Step 3: Check expiry (time-based)
        if (record.isExpired()) {
            record.markExpired();
            otpRecordRepository.save(record);
            log.info("OTP expired for referenceId: {}", request.getReferenceId());
            return OtpVerifyResponse.builder()
                    .verified(false)
                    .message("OTP has expired. Please request a new one.")
                    .build();
        }

        // Step 4: Check max attempts
        if (record.hasExceededMaxAttempts(otpProperties.getMaxAttempts())) {
            record.markBlocked();
            otpRecordRepository.save(record);
            log.warn("OTP blocked due to max attempts for referenceId: {}", request.getReferenceId());
            return OtpVerifyResponse.builder()
                    .verified(false)
                    .message("Maximum verification attempts exceeded. OTP has been blocked.")
                    .build();
        }

        // Step 5: Verify the OTP hash (timing-safe comparison via BCrypt)
        boolean isValid = otpHashingService.verifyOtp(request.getOtp(), record.getOtpHash());

        if (isValid) {
            // Success! Mark as verified
            record.markVerified();
            otpRecordRepository.save(record);
            log.info("OTP verified successfully for referenceId: {}", request.getReferenceId());
            return OtpVerifyResponse.builder()
                    .verified(true)
                    .message("OTP verified successfully.")
                    .build();
        } else {
            // Wrong OTP — increment attempt counter
            record.incrementAttempts();
            otpRecordRepository.save(record);

            int attemptsRemaining = otpProperties.getMaxAttempts() - record.getAttemptCount();
            log.warn("Invalid OTP for referenceId: {}. Attempts remaining: {}",
                    request.getReferenceId(), attemptsRemaining);

            // If this attempt was the last one, mark as blocked
            if (attemptsRemaining <= 0) {
                record.markBlocked();
                otpRecordRepository.save(record);
                return OtpVerifyResponse.builder()
                        .verified(false)
                        .attemptsRemaining(0)
                        .message("Maximum verification attempts exceeded. OTP has been blocked.")
                        .build();
            }

            return OtpVerifyResponse.builder()
                    .verified(false)
                    .attemptsRemaining(attemptsRemaining)
                    .message("Invalid OTP. " + attemptsRemaining + " attempt(s) remaining.")
                    .build();
        }
    }

    /**
     * Invalidate all ACTIVE OTPs for a given identifier.
     *
     * This is called before generating a new OTP to ensure only one
     * ACTIVE OTP exists per identifier at any time.
     *
     * Why? Imagine a user requests 3 OTPs. Without invalidation,
     * all 3 would be valid simultaneously. The user wouldn't know
     * which one to use, and an attacker would have 3x the attack surface.
     */
    private void invalidateExistingOtps(String identifier) {
        List<OtpRecord> activeOtps = otpRecordRepository
                .findByIdentifierAndStatus(identifier, OtpStatus.ACTIVE);

        if (!activeOtps.isEmpty()) {
            log.info("Invalidating {} existing ACTIVE OTP(s) for identifier: {}",
                    activeOtps.size(), maskIdentifier(identifier));
            activeOtps.forEach(OtpRecord::markExpired);
            otpRecordRepository.saveAll(activeOtps);
        }
    }

    /**
     * Get the appropriate notification service for the channel.
     *
     * Uses the Strategy Map populated during construction for O(1) lookup.
     *
     * @param channel the channel name (e.g., "SMS", "EMAIL", "CONSOLE")
     * @return the corresponding NotificationService implementation
     * @throws OtpException if no implementation exists for the channel
     */
    private NotificationService getNotificationService(String channel) {
        NotificationService service = notificationServiceMap.get(channel);
        if (service == null) {
            throw new OtpException("Unsupported notification channel: " + channel);
        }
        return service;
    }

    /**
     * Mask an identifier for secure logging.
     *
     * Security: Never log full phone numbers or emails in production.
     * This prevents sensitive data from appearing in log files.
     *
     * Examples:
     *   "+919876543210" → "+91****3210"
     *   "user@example.com" → "us***@example.com"
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 6) {
            return "****";
        }
        int visibleStart = 3;
        int visibleEnd = 4;
        return identifier.substring(0, visibleStart) +
                "****" +
                identifier.substring(identifier.length() - visibleEnd);
    }
}
