package com.otpservice.controller;

import com.otpservice.dto.OtpGenerateRequest;
import com.otpservice.dto.OtpGenerateResponse;
import com.otpservice.dto.OtpVerifyRequest;
import com.otpservice.dto.OtpVerifyResponse;
import com.otpservice.service.OtpService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing OTP generation and verification endpoints.
 *
 * ═══════════════════════════════════════════════════════════════
 * DESIGN DEEP DIVE: Controller Responsibilities
 * ═══════════════════════════════════════════════════════════════
 *
 * The controller should be THIN — it only handles:
 *   1. Accepting HTTP requests and deserializing JSON → DTOs
 *   2. Delegating to the service layer
 *   3. Returning the response with appropriate HTTP status
 *
 * All business logic lives in OtpService. This separation ensures:
 *   - Controllers are easy to test (just HTTP plumbing)
 *   - Business logic can be reused (e.g., called from a message queue listener)
 *   - Single Responsibility Principle
 *
 * ═══════════════════════════════════════════════════════════════
 * API VERSIONING: /api/v1/otp
 * ═══════════════════════════════════════════════════════════════
 *
 * URL-based versioning (/v1, /v2) is the most common approach for REST APIs.
 * When you need breaking changes, create /api/v2/otp while keeping /v1 active
 * for backward compatibility.
 */
@RestController
@RequestMapping("/api/v1/otp")
@Tag(name = "OTP", description = "One-Time Password generation and verification")
public class OtpController {

    private static final Logger log = LoggerFactory.getLogger(OtpController.class);

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    /**
     * Generate and deliver a new OTP.
     *
     * @param request contains identifier, channel, and purpose
     * @return referenceId and expiry details
     */
    @PostMapping("/generate")
    @Operation(
            summary = "Generate OTP",
            description = "Generates a new OTP for the given identifier and delivers it via the specified channel. " +
                    "Any existing active OTPs for the same identifier are automatically invalidated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP generated and delivered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request (validation error)"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<OtpGenerateResponse> generateOtp(
            @Valid @RequestBody OtpGenerateRequest request) {

        log.info("POST /api/v1/otp/generate - Channel: {}, Purpose: {}",
                request.getChannel(), request.getPurpose());

        OtpGenerateResponse response = otpService.generateOtp(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Verify a user-submitted OTP against the stored hash.
     *
     * @param request contains referenceId and the OTP to verify
     * @return verification result with remaining attempts
     */
    @PostMapping("/verify")
    @Operation(
            summary = "Verify OTP",
            description = "Verifies the submitted OTP against the stored hash for the given reference ID. " +
                    "Returns success/failure with remaining attempts on failure."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification result returned"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Invalid reference ID")
    })
    public ResponseEntity<OtpVerifyResponse> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request) {

        log.info("POST /api/v1/otp/verify - ReferenceId: {}", request.getReferenceId());

        OtpVerifyResponse response = otpService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }
}
