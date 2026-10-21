package com.klabis.members;

import com.klabis.members.domain.Member;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a Member's membership is suspended.
 *
 * <p>Carries only the member id; listeners load anything else through the members module's ports.
 */
@RecordBuilder
@DomainEvent
public record MemberSuspendedEvent(
        UUID eventId,
        MemberId memberId,
        Instant suspendedAt
) {

    public MemberSuspendedEvent(MemberId memberId, Instant suspendedAt) {
        this(UUID.randomUUID(), memberId, suspendedAt);
    }

    public MemberSuspendedEvent {
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(memberId, "Member ID is required");
        Objects.requireNonNull(suspendedAt, "Suspended at timestamp is required");
    }

    public static MemberSuspendedEvent fromAggregate(Member member) {
        return new MemberSuspendedEvent(member.getId(), member.getSuspendedAt());
    }
}
