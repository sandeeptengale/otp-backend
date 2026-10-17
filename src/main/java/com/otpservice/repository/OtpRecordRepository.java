package com.otpservice.repository;

import com.otpservice.entity.OtpRecord;
import com.otpservice.enums.OtpStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for OTP record persistence operations.
 *
 * Design Note (Repository Pattern):
 * Spring Data JPA auto-generates the implementation at runtime based on
 * method names. This eliminates boilerplate JDBC/SQL code and provides
 * type-safe query methods.
 *
 * Method naming convention:
 *   findBy<Field>And<Field> → SELECT * FROM otp_records WHERE field = ? AND field = ?
 *   countBy<Field>And<Field>... → SELECT COUNT(*) FROM otp_records WHERE ...
 */
@Repository
public interface OtpRecordRepository extends JpaRepository<OtpRecord, Long> {

    /**
     * Find an OTP record by its public reference ID.
     * Primary lookup method used during verification.
     */
    Optional<OtpRecord> findByReferenceId(String referenceId);

    /**
     * Find the most recent ACTIVE OTP for a given identifier.
     * Used to invalidate existing OTPs before generating a new one.
     */
    List<OtpRecord> findByIdentifierAndStatus(String identifier, OtpStatus status);

    /**
     * Count OTPs generated for an identifier within a time window.
     * Used for rate-limit enforcement on OTP generation.
     *
     * Example: "How many OTPs were generated for +919876543210 in the last 10 minutes?"
     */
    long countByIdentifierAndCreatedAtAfter(String identifier, LocalDateTime after);

    /**
     * Find all expired but still ACTIVE OTP records.
     * Used by the cleanup scheduler to transition them to EXPIRED status.
     */
    List<OtpRecord> findByStatusAndExpiresAtBefore(OtpStatus status, LocalDateTime before);
}
