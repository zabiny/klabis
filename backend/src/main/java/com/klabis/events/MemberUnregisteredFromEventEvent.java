package com.klabis.events;

import com.klabis.members.MemberId;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a member unregisters from an event.
 */
@DomainEvent
public class MemberUnregisteredFromEventEvent {

    private final UUID occurrenceId;
    private final EventId eventId;
    private final MemberId memberId;
    private final Instant occurredAt;

    private MemberUnregisteredFromEventEvent(UUID occurrenceId, EventId eventId, MemberId memberId, Instant occurredAt) {
        this.occurrenceId = Objects.requireNonNull(occurrenceId, "Occurrence ID is required");
        this.eventId = Objects.requireNonNull(eventId, "Event ID is required");
        this.memberId = Objects.requireNonNull(memberId, "Member ID is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    public static MemberUnregisteredFromEventEvent fromAggregate(Event event, MemberId memberId) {
        return new MemberUnregisteredFromEventEvent(UUID.randomUUID(), event.getId(), memberId, Instant.now());
    }

    public UUID getOccurrenceId() {
        return occurrenceId;
    }

    public EventId eventId() {
        return eventId;
    }

    public MemberId memberId() {
        return memberId;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberUnregisteredFromEventEvent that = (MemberUnregisteredFromEventEvent) o;
        return Objects.equals(occurrenceId, that.occurrenceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(occurrenceId);
    }

    @Override
    public String toString() {
        return "MemberUnregisteredFromEventEvent{" +
               "occurrenceId=" + occurrenceId +
               ", eventId=" + eventId +
               ", memberId=" + memberId +
               ", occurredAt=" + occurredAt +
               '}';
    }
}
