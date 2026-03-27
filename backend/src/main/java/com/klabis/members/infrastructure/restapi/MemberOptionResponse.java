package com.klabis.members.infrastructure.restapi;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lightweight DTO for member selection in UI components (dropdowns, multiselects).
 * <p>
 * Follows HAL+Forms options contract: {@code value} holds the identifier
 * and {@code prompt} holds the human-readable display text.
 */
@Schema(description = "Member option for select components")
record MemberOptionResponse(
        @Schema(description = "Member UUID", example = "123e4567-e89b-12d3-a456-426614174000")
        String value,

        @Schema(description = "Display name with registration number", example = "Jan Novák (ZBM0001)")
        String prompt
) {
}
