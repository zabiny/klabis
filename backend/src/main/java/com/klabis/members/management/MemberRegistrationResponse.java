package com.klabis.members.management;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Response DTO for member registration endpoint.
 * <p>
 * Represents the JSON payload returned by POST /api/members
 * Includes basic member information and will be enriched with HATEOAS links.
 */
@Schema(description = "Member registration response with HATEOAS links")
record MemberRegistrationResponse(
        @Schema(description = "Unique member identifier (UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Member's first name", example = "Jan")
        String firstName,

        @Schema(description = "Member's last name", example = "Novák")
        String lastName
) {
}
