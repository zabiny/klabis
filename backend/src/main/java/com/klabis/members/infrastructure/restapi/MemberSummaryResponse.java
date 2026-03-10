package com.klabis.members.infrastructure.restapi;

import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for member summary in list endpoints.
 * <p>
 * Represents the JSON payload returned for each member in GET /api/members.
 * Contains minimal member information: firstName, lastName, and registrationNumber.
 * <p>
 * This response will be enriched with HATEOAS links when returned as part of a collection.
 */
@RecordBuilder
@Schema(description = "Member summary with essential information and HATEOAS links")
public record MemberSummaryResponse(
        @Schema(description = "Unique member identifier (UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
        MemberId id,

        @Schema(description = "Member's first name", example = "Jan")
        String firstName,

        @Schema(description = "Member's last name", example = "Novák")
        String lastName,

        @Schema(description = "Member's unique registration number", example = "ZBM0501")
        String registrationNumber
) {
}
