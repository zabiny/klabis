package com.klabis.users.passwordsetup;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for setting a password.
 *
 * @param token                the plain text token from email
 * @param password             the new password
 * @param passwordConfirmation password confirmation
 */
@Schema(description = "Password setup request containing token and new credentials")
public record SetPasswordRequest(
        @NotBlank(message = "Token is required")
        @Schema(description = "The plain text token from email", example = "abc123def456", requiredMode = Schema.RequiredMode.REQUIRED)
        String token,

        @NotBlank(message = "Password is required")
        @Schema(description = "The new password (minimum 12 characters with uppercase, lowercase, number, and special character)", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String password,

        @NotBlank(message = "Password confirmation is required")
        @Schema(description = "Password confirmation must match the password field", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String passwordConfirmation
) {
}
