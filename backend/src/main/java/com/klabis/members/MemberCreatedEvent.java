package com.klabis.members;

import com.klabis.members.domain.Member;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a new Member is created.
 *
 * <p>Carries only what current listeners need (member id and date of birth); listeners load
 * anything else through the members module's ports.
 */
@RecordBuilder
@DomainEvent
public record MemberCreatedEvent(
        UUID eventId,
        MemberId memberId,
        LocalDate dateOfBirth,
        Instant occurredAt
) {

    public MemberCreatedEvent(MemberId memberId, LocalDate dateOfBirth) {
        this(UUID.randomUUID(), memberId, dateOfBirth, Instant.now());
    }

    public MemberCreatedEvent {
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(memberId, "Member ID is required");
        Objects.requireNonNull(dateOfBirth, "Date of birth is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    public static MemberCreatedEvent fromAggregate(Member member) {
        return new MemberCreatedEvent(member.getId(), member.getDateOfBirth());
    }
}
