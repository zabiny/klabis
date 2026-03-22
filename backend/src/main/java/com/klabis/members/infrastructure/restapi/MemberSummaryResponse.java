package com.klabis.members.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.common.security.fieldsecurity.NullDeniedHandler;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;

/**
 * Response DTO for member summary in list endpoints.
 * <p>
 * Represents the JSON payload returned for each member in GET /api/members.
 * Contains minimal member information: firstName, lastName, and registrationNumber.
 * Sensitive fields (email, active) are only visible to MEMBERS:MANAGE authority holders.
 * <p>
 * This response will be enriched with HATEOAS links when returned as part of a collection.
 */
@RecordBuilder
@Schema(description = "Member summary with essential information and HATEOAS links")
@JsonInclude(JsonInclude.Include.NON_NULL)
@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
public record MemberSummaryResponse(
        @Schema(description = "Unique member identifier (UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
        MemberId id,

        @Schema(description = "Member's first name", example = "Jan")
        String firstName,

        @Schema(description = "Member's last name", example = "Novák")
        String lastName,

        @Schema(description = "Member's unique registration number", example = "ZBM0501")
        String registrationNumber,

        @HasAuthority(Authority.MEMBERS_MANAGE)
        @Schema(description = "Member's email address (visible to admins only)")
        String email,

        @HasAuthority(Authority.MEMBERS_MANAGE)
        @Schema(description = "Whether the member is active (visible to admins only)")
        Boolean active
) {
}
