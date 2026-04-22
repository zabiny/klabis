package com.klabis.events;

import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.SiCardNumber;
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
        SiCardNumber oldSiCardNumber,
        String oldCategory,
        SiCardNumber newSiCardNumber,
        String newCategory,
        Instant occurredAt
) {

    public RegistrationEditedEvent {
        Objects.requireNonNull(occurrenceId, "Occurrence ID is required");
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(memberId, "Member ID is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    public static RegistrationEditedEvent fromAggregate(Event event, MemberId memberId,
                                                         EventRegistration oldRegistration,
                                                         EventRegistration newRegistration) {
        return new RegistrationEditedEvent(
                UUID.randomUUID(),
                event.getId(),
                memberId,
                oldRegistration.siCardNumber(),
                oldRegistration.category(),
                newRegistration.siCardNumber(),
                newRegistration.category(),
                Instant.now()
        );
    }
}
