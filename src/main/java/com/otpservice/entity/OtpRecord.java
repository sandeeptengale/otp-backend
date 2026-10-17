package com.otpservice.entity;

import com.otpservice.enums.ChannelType;
import com.otpservice.enums.OtpPurpose;
import com.otpservice.enums.OtpStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing an OTP record in the database.
 *
 * Design Decisions:
 *
 * 1. HASHED OTP STORAGE:
 *    The 'otpHash' field stores a BCrypt hash of the OTP, NEVER the plain text.
 *    If the database is compromised, attackers cannot recover active OTPs.
 *
 * 2. REFERENCE ID:
 *    A UUID-based reference ID is returned to the client instead of exposing
 *    the database primary key. This prevents enumeration attacks (guessing IDs).
 *
 * 3. ATTEMPT TRACKING:
 *    'attemptCount' is incremented on each failed verification. When it reaches
 *    the configured max, the OTP transitions to BLOCKED status.
 *
 * 4. AUDIT FIELDS:
 *    'createdAt', 'expiresAt', and 'verifiedAt' provide a complete audit trail
 *    for security investigations and compliance.
 *
 * 5. DATABASE INDEXES:
 *    Indexes on 'referenceId' and 'identifier + status' ensure fast lookups
 *    for the two most common query patterns: verification and rate-limit checks.
 */
@Entity
@Table(
    name = "otp_records",
    indexes = {
        @Index(name = "idx_reference_id", columnList = "referenceId", unique = true),
        @Index(name = "idx_identifier_status", columnList = "identifier, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public-facing identifier for this OTP session.
     * Returned to the client; used for verification lookups.
     * UUID v4 ensures unpredictability (no sequential IDs to enumerate).
     */
    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String referenceId;

    /**
     * The target identifier (phone number, email, etc.) that received the OTP.
     * Used for rate-limit checks and business logic grouping.
     */
    @Column(nullable = false, length = 100)
    private String identifier;

    /**
     * BCrypt hash of the generated OTP.
     * NEVER store the plain-text OTP in the database.
     */
    @Column(nullable = false, length = 256)
    private String otpHash;

    /**
     * Current lifecycle status of this OTP.
     * @see OtpStatus for state transition rules.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpStatus status;

    /**
     * The delivery channel used for this OTP.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChannelType channel;

    /**
     * The purpose for which this OTP was generated.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpPurpose purpose;

    /**
     * Number of failed verification attempts.
     * Incremented on each wrong OTP submission.
     */
    @Column(nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    /**
     * Timestamp when this OTP was created.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this OTP expires.
     * Computed as: createdAt + configured expiry duration.
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Timestamp when this OTP was successfully verified.
     * Null if not yet verified.
     */
    private LocalDateTime verifiedAt;

    /**
     * Check if this OTP has expired based on the current time.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if this OTP has exceeded the maximum allowed verification attempts.
     */
    public boolean hasExceededMaxAttempts(int maxAttempts) {
        return attemptCount >= maxAttempts;
    }

    /**
     * Increment the failed attempt counter.
     */
    public void incrementAttempts() {
        this.attemptCount++;
    }

    /**
     * Transition this OTP to VERIFIED status.
     */
    public void markVerified() {
        this.status = OtpStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
    }

    /**
     * Transition this OTP to EXPIRED status.
     */
    public void markExpired() {
        this.status = OtpStatus.EXPIRED;
    }

    /**
     * Transition this OTP to BLOCKED status.
     */
    public void markBlocked() {
        this.status = OtpStatus.BLOCKED;
    }

    /**
     * Factory method to create a new OTP record with default values.
     */
    public static OtpRecord create(String identifier, String otpHash,
                                    ChannelType channel, OtpPurpose purpose,
                                    int expirySeconds) {
        LocalDateTime now = LocalDateTime.now();
        return OtpRecord.builder()
                .referenceId(UUID.randomUUID().toString())
                .identifier(identifier)
                .otpHash(otpHash)
                .status(OtpStatus.ACTIVE)
                .channel(channel)
                .purpose(purpose)
                .attemptCount(0)
                .createdAt(now)
                .expiresAt(now.plusSeconds(expirySeconds))
                .build();
    }
}
