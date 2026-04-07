package com.klabis.events.infrastructure.jdbc;

import com.klabis.events.domain.EventRegistration;
import com.klabis.members.MemberId;
import com.klabis.events.domain.SiCardNumber;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.UUID;

/**
 * Memento for EventRegistration entity persistence.
 * <p>
 * Represents a row in the event_registrations table as part of the Event aggregate.
 * This class handles the conversion between the domain EventRegistration entity
 * and the database representation.
 */
@Table("event_registrations")
class EventRegistrationMemento {

    @Column("id")
    private UUID id;

    // event_id is managed by @MappedCollection in EventMemento
    // Spring Data JDBC automatically sets it

    @Column("member_id")
    private UUID memberId;

    @Column("si_card_number")
    private String siCardNumber;

    @Column("category")
    private String category;

    @Column("registered_at")
    private Instant registeredAt;

    /**
     * Default constructor required by Spring Data JDBC.
     */
    protected EventRegistrationMemento() {
    }

    /**
     * Creates an EventRegistrationMemento from an EventRegistration entity.
     *
     * @param registration the EventRegistration entity to convert
     * @return a new EventRegistrationMemento
     */
    static EventRegistrationMemento from(EventRegistration registration) {
        Assert.notNull(registration, "EventRegistration must not be null");

        EventRegistrationMemento memento = new EventRegistrationMemento();
        memento.id = registration.id();
        memento.memberId = registration.memberId().uuid();
        memento.siCardNumber = registration.siCardNumber().value();
        memento.category = registration.category();
        memento.registeredAt = registration.registeredAt();

        return memento;
    }

    /**
     * Converts this EventRegistrationMemento to an EventRegistration entity.
     *
     * @return a reconstituted EventRegistration entity
     */
    EventRegistration toEventRegistration() {
        return EventRegistration.reconstruct(
                this.id,
                new MemberId(this.memberId),
                new SiCardNumber(this.siCardNumber),
                this.category,
                this.registeredAt
        );
    }

    // Package-private getters for Spring Data JDBC
    UUID getId() {
        return id;
    }

    UUID getMemberId() {
        return memberId;
    }

    String getSiCardNumber() {
        return siCardNumber;
    }

    String getCategory() {
        return category;
    }

    Instant getRegisteredAt() {
        return registeredAt;
    }
}
