package com.klabis.members.infrastructure.restapi;

import com.klabis.members.domain.DeactivationReason;

import java.time.Instant;

/**
 * Response DTO for membership termination.
 * <p>
 * Contains the details of a terminated membership including when and why it was terminated.
 * </p>
 *
 * @param reason       the reason for membership termination
 * @param deactivatedAt the timestamp when the membership was deactivated
 * @param deactivatedBy the ID of the user who performed the termination
 * @param note         optional note providing additional context about the termination
 */
public record MembershipTerminationResponse(
        DeactivationReason reason,
        Instant deactivatedAt,
        String deactivatedBy,
        String note
) {
}
