package com.klabis.users.passwordsetup;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for token request.
 *
 * @param message success message
 */
@Schema(description = "Response for password setup token request")
public record TokenRequestResponse(
        @Schema(description = "Success message", example = "If your account is pending activation, you will receive an email with a new setup link.")
        String message
) {
}
