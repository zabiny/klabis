package com.klabis.members.management;

import com.klabis.members.DeactivationReason;
import com.klabis.common.validation.ValidOptionalNotBlank;
import com.klabis.common.validation.ValidOptionalSize;

import java.util.Optional;

/**
 * Request DTO for terminating a member's membership.
 * <p>
 * This DTO is used for POST requests to terminate a member's membership.
 * </p>
 * Validation rules:
 * - reason: required, must be a valid DeactivationReason enum value
 * - note: optional, max 500 characters if provided
 *
 * @param reason the reason for membership termination (required)
 * @param note   optional note providing additional context about the termination
 */
public record TerminateMembershipRequest(
        DeactivationReason reason,

        @ValidOptionalSize(max = 500, message = "Termination note must not exceed 500 characters")
        Optional<String> note
) {
    /**
     * Creates a new TerminateMembershipRequest.
     *
     * @param reason the reason for termination (required)
     * @param note   optional note (max 500 characters)
     */
    public TerminateMembershipRequest {
        if (reason == null) {
            throw new IllegalArgumentException("Deactivation reason is required");
        }
    }
}
