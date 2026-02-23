package com.klabis.common.users.passwordsetup;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for password setup completion.
 *
 * @param message            success message
 * @param registrationNumber the user's registration number
 */
@Schema(description = "Response for completed password setup")
public record PasswordSetupResponse(
        @Schema(description = "Success message", example = "Password set successfully")
        String message,
        @Schema(description = "The user's registration number", example = "12345678")
        String registrationNumber
) {
}
