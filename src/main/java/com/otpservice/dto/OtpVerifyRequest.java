package com.otpservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for OTP verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyRequest {

    /**
     * The reference ID returned during OTP generation.
     * Must be a valid UUID format.
     */
    @NotBlank(message = "Reference ID is required")
    private String referenceId;

    /**
     * The OTP entered by the user.
     * Must contain only digits and match the configured OTP length.
     */
    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{4,8}$", message = "OTP must be 4-8 digits")
    private String otp;
}
