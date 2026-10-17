package com.otpservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for OTP verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpVerifyResponse {

    /** Whether the OTP was successfully verified. */
    private boolean verified;

    /** Human-readable result message. */
    private String message;

    /**
     * Number of remaining verification attempts (null if verified or terminal state).
     * This helps the client show a meaningful message like "2 attempts remaining".
     */
    private Integer attemptsRemaining;
}
