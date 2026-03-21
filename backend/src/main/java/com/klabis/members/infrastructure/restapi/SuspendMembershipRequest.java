package com.klabis.members.infrastructure.restapi;

import com.klabis.common.validation.ValidOptionalSize;
import com.klabis.members.domain.DeactivationReason;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

/**
 * Request DTO for suspending a member's membership.
 *
 * @param reason the reason for membership suspension (required)
 * @param note   optional note providing additional context about the suspension
 */
public record SuspendMembershipRequest(
        @NotNull(message = "Důvod je povinný")
        DeactivationReason reason,

        @ValidOptionalSize(max = 500, message = "Poznámka nesmí přesáhnout 500 znaků")
        Optional<String> note
) {
}
