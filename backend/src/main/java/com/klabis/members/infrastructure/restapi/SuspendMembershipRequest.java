package com.klabis.members.infrastructure.restapi;

import com.klabis.common.validation.ValidOptionalSize;
import com.klabis.members.domain.DeactivationReason;

import java.util.Optional;

/**
 * Request DTO for suspending a member's membership.
 *
 * @param reason the reason for membership suspension (required)
 * @param note   optional note providing additional context about the suspension
 */
public record SuspendMembershipRequest(
        DeactivationReason reason,

        @ValidOptionalSize(max = 500, message = "Suspension note must not exceed 500 characters")
        Optional<String> note
) {
    public SuspendMembershipRequest {
        if (reason == null) {
            throw new IllegalArgumentException("Deactivation reason is required");
        }
    }
}
