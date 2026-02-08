package com.klabis.users.passwordsetup;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Error response DTO.
 *
 * @param message error message
 */
@Schema(description = "Error response for password setup API errors")
public record ErrorResponse(
        @Schema(description = "Error message describing what went wrong", example = "Invalid token")
        String message
) {
}
