package com.klabis.events;

import com.klabis.events.domain.Event;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a member unregisters from an event.
 */
@RecordBuilder
@DomainEvent
public record MemberUnregisteredFromEventEvent(
        UUID occurrenceId,
        EventId eventId,
        MemberId memberId,
        Instant occurredAt
) {

    public MemberUnregisteredFromEventEvent {
        Objects.requireNonNull(occurrenceId, "Occurrence ID is required");
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(memberId, "Member ID is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    public static MemberUnregisteredFromEventEvent fromAggregate(Event event, MemberId memberId) {
        return new MemberUnregisteredFromEventEvent(UUID.randomUUID(), event.getId(), memberId, Instant.now());
    }
}
