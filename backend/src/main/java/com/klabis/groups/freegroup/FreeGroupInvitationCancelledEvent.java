package com.klabis.groups.freegroup;

import com.klabis.common.usergroup.InvitationId;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
