package com.klabis.common.users.infrastructure.restapi;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for requesting a new token.
 *
 * @param registrationNumber the registration number
 * @param email              the member's email address
 */
@Schema(description = "Request for a new password setup token")
public record TokenRequestRequest(
        @NotBlank(message = "Registration number is required")
        @Schema(description = "The user's registration number (format: XXXYYDD)", example = "12345678", pattern = "\\d{8}", requiredMode = Schema.RequiredMode.REQUIRED)
        String registrationNumber,

        @NotBlank(message = "Email is required")
        @Schema(description = "The member's email address", example = "member@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email
) {
}
