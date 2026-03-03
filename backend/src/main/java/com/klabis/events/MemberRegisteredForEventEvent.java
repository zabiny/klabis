package com.klabis.events;

import com.klabis.events.domain.Event;
import com.klabis.members.MemberId;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a member registers for an event.
 */
@DomainEvent
public record MemberRegisteredForEventEvent(
        UUID occurrenceId,
        EventId eventId,
        MemberId memberId,
        Instant occurredAt
) {

    public MemberRegisteredForEventEvent {
        Objects.requireNonNull(occurrenceId, "Occurrence ID is required");
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(memberId, "Member ID is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    public static MemberRegisteredForEventEvent fromAggregate(Event event, MemberId memberId) {
        return new MemberRegisteredForEventEvent(UUID.randomUUID(), event.getId(), memberId, Instant.now());
    }
}
