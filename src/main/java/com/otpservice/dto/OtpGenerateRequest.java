package com.otpservice.dto;

import com.otpservice.enums.ChannelType;
import com.otpservice.enums.OtpPurpose;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for OTP generation.
 *
 * Design Note:
 * DTOs (Data Transfer Objects) decouple the API contract from internal entities.
 * This means we can change the internal database schema without breaking the API,
 * and vice versa. The client never sees internal fields like 'otpHash' or 'id'.
 *
 * Jakarta Bean Validation annotations (@NotBlank, @NotNull, @Size) provide
 * declarative input validation — Spring automatically returns 400 Bad Request
 * with descriptive error messages if validation fails.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpGenerateRequest {

    /**
     * The target identifier to send the OTP to.
     * Could be a phone number (e.g., "+919876543210") or email address.
     */
    @NotBlank(message = "Identifier is required (phone number or email)")
    @Size(min = 5, max = 100, message = "Identifier must be between 5 and 100 characters")
    private String identifier;

    /**
     * The delivery channel for the OTP.
     * Use CONSOLE for local development (prints OTP to application log).
     */
    @NotNull(message = "Channel is required (SMS, EMAIL, or CONSOLE)")
    private ChannelType channel;

    /**
     * The purpose of this OTP.
     * Enables purpose-specific rate limits and audit trails.
     */
    @NotNull(message = "Purpose is required (LOGIN, REGISTRATION, PASSWORD_RESET, or TRANSACTION)")
    private OtpPurpose purpose;
}
