package com.klabis.events;

import com.klabis.common.users.UserId;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event published when a member registers for an event.
 */
@DomainEvent
public class MemberRegisteredForEventEvent {

    private final EventId eventId;
    private final UserId memberId;
    private final Instant occurredAt;

    public MemberRegisteredForEventEvent(EventId eventId, UserId memberId) {
        this(eventId, memberId, Instant.now());
    }

    public MemberRegisteredForEventEvent(EventId eventId, UserId memberId, Instant occurredAt) {
        this.eventId = Objects.requireNonNull(eventId, "Event ID is required");
        this.memberId = Objects.requireNonNull(memberId, "Member ID is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    public EventId eventId() {
        return eventId;
    }

    public UserId memberId() {
        return memberId;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberRegisteredForEventEvent that = (MemberRegisteredForEventEvent) o;
        return Objects.equals(eventId, that.eventId) &&
               Objects.equals(memberId, that.memberId) &&
               Objects.equals(occurredAt, that.occurredAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, memberId, occurredAt);
    }

    @Override
    public String toString() {
        return "MemberRegisteredForEventEvent{" +
               "eventId=" + eventId +
               ", memberId=" + memberId +
               ", occurredAt=" + occurredAt +
               '}';
    }
}
