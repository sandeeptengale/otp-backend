package com.otpservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for OTP generation.
 *
 * Notice what is NOT included:
 * - The OTP itself (it was sent via the channel, never in the API response)
 * - Internal database ID
 * - The OTP hash
 *
 * Only the referenceId (needed for verification) and metadata are returned.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpGenerateResponse {

    /** UUID reference for this OTP session. Used in the verify request. */
    private String referenceId;

    /** Time remaining (in seconds) before this OTP expires. */
    private int expiresInSeconds;

    /** Human-readable success message. */
    private String message;
}
