package com.klabis.events;

import com.klabis.events.domain.Event;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@RecordBuilder
@DomainEvent
public record RegistrationEditedEvent(
        UUID occurrenceId,
        EventId eventId,
        MemberId memberId,
        Instant occurredAt,
        UpdateOrigin origin
) {

    public RegistrationEditedEvent {
        Objects.requireNonNull(occurrenceId, "Occurrence ID is required");
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(memberId, "Member ID is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
        Objects.requireNonNull(origin, "Update origin is required");
    }

    public static RegistrationEditedEvent fromAggregate(Event event, MemberId memberId, UpdateOrigin origin) {
        return new RegistrationEditedEvent(UUID.randomUUID(), event.getId(), memberId, Instant.now(), origin);
    }
}
