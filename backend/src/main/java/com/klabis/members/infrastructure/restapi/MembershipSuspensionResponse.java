package com.klabis.members.infrastructure.restapi;

import com.klabis.members.domain.DeactivationReason;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.time.Instant;

/**
 * Response DTO for membership suspension.
 *
 * @param reason      the reason for membership suspension
 * @param suspendedAt the timestamp when the membership was suspended
 * @param suspendedBy the ID of the user who performed the suspension
 * @param note        optional note providing additional context about the suspension
 */
@RecordBuilder
public record MembershipSuspensionResponse(
        DeactivationReason reason,
        Instant suspendedAt,
        String suspendedBy,
        String note
) {
}
