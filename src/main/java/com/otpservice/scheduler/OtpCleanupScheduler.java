package com.otpservice.scheduler;

import com.otpservice.entity.OtpRecord;
import com.otpservice.enums.OtpStatus;
import com.otpservice.repository.OtpRecordRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to clean up expired OTP records.
 *
 * ═══════════════════════════════════════════════════════════════
 * DESIGN DEEP DIVE: Why a Cleanup Scheduler?
 * ═══════════════════════════════════════════════════════════════
 *
 * OTPs are checked for expiry during verification (lazy evaluation).
 * But what about OTPs that are never verified?
 *
 * Without cleanup:
 *   - The database accumulates stale ACTIVE records indefinitely
 *   - These records still show as "ACTIVE" even though they're expired
 *   - Rate-limit queries slow down as the table grows
 *   - Security audits show misleading data
 *
 * The scheduler periodically finds all ACTIVE OTPs whose expiresAt
 * timestamp has passed and transitions them to EXPIRED status.
 *
 * In production with Redis, you'd use Redis TTL for automatic expiry.
 * With a relational DB, this scheduler approach is standard.
 */
@Component
public class OtpCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(OtpCleanupScheduler.class);

    private final OtpRecordRepository otpRecordRepository;

    public OtpCleanupScheduler(OtpRecordRepository otpRecordRepository) {
        this.otpRecordRepository = otpRecordRepository;
    }

    /**
     * Runs on the configured cron schedule (default: every 5 minutes).
     *
     * Finds all ACTIVE OTP records that have passed their expiry time
     * and transitions them to EXPIRED status.
     */
    @Scheduled(cron = "${otp.cleanup-cron}")
    @Transactional
    public void cleanupExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        List<OtpRecord> expiredRecords = otpRecordRepository
                .findByStatusAndExpiresAtBefore(OtpStatus.ACTIVE, now);

        if (expiredRecords.isEmpty()) {
            log.debug("OTP cleanup: no expired records found");
            return;
        }

        expiredRecords.forEach(OtpRecord::markExpired);
        otpRecordRepository.saveAll(expiredRecords);

        log.info("OTP cleanup: marked {} expired OTP record(s) as EXPIRED", expiredRecords.size());
    }
}
