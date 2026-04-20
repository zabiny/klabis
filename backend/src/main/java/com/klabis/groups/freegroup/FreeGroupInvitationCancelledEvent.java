package com.klabis.groups.freegroup;

import com.klabis.common.usergroup.InvitationId;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Emitted when a pending free-group invitation is cancelled by a current owner or SYSTEM.
 * <p>
 * {@code actor} is empty when the cancellation was triggered by SYSTEM (e.g. invitee deactivation).
 * {@code recipientOwnerIds} is snapshotted at emit time: all current owners except the actor,
 * or all owners when the actor is SYSTEM — so downstream notification delivery never needs
 * to query aggregate state again.
 */
@RecordBuilder
@DomainEvent
public record FreeGroupInvitationCancelledEvent(
        UUID eventId,
        FreeGroupId groupId,
        InvitationId invitationId,
        MemberId inviteeMemberId,
        Optional<MemberId> actor,
        Optional<String> reason,
        Set<MemberId> recipientOwnerIds,
        Instant cancelledAt
) {
}
