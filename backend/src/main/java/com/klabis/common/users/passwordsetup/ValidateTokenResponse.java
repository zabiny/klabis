package com.klabis.common.users.passwordsetup;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response DTO for token validation.
 *
 * @param valid     whether the token is valid
 * @param expiresAt when the token expires
 */
@Schema(description = "Response for token validation")
public record ValidateTokenResponse(
        @Schema(description = "Whether the token is valid", example = "true")
        boolean valid,
        @Schema(description = "When the token expires (ISO-8601 format)", example = "2024-12-31T23:59:59Z")
        Instant expiresAt
) {
}
