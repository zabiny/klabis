package com.klabis.events.infrastructure.jdbc;

import com.klabis.events.EventCategoryId;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventRegistrationCreateEventRegistrationBuilder;
import com.klabis.events.domain.RegistrationNote;
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
@Table(schema = "events", value = "event_registrations")
class EventRegistrationMemento {

    @Column("id")
    private UUID id;

    // event_id is managed by @MappedCollection in EventMemento
    // Spring Data JDBC automatically sets it

    @Column("member_id")
    private UUID memberId;

    @Column("si_card_number")
    private String siCardNumber;

    @Column("category_id")
    private UUID categoryId;

    @Column("registered_at")
    private Instant registeredAt;

    @Column("wants_shared_transport")
    private boolean wantsSharedTransport;

    @Column("wants_shared_accommodation")
    private boolean wantsSharedAccommodation;

    @Column("wants_to_rent_si")
    private boolean wantsToRentSi;

    @Column("internal_note")
    private String internalNote;

    @Column("oris_note")
    private String orisNote;

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
        memento.siCardNumber = registration.siCardNumber() != null ? registration.siCardNumber().value() : null;
        memento.wantsToRentSi = registration.wantsToRentSi();
        memento.categoryId = registration.categoryId() != null ? registration.categoryId().value() : null;
        memento.registeredAt = registration.registeredAt();
        memento.wantsSharedTransport = registration.wantsSharedTransport();
        memento.wantsSharedAccommodation = registration.wantsSharedAccommodation();
        memento.internalNote = RegistrationNote.textOrNull(registration.internalNote());
        memento.orisNote = RegistrationNote.textOrNull(registration.orisNote());

        return memento;
    }

    /**
     * Converts this EventRegistrationMemento to an EventRegistration entity.
     *
     * @return a reconstituted EventRegistration entity
     */
    EventRegistration toEventRegistration() {
        return EventRegistration.reconstruct(this.id, this.registeredAt,
                EventRegistrationCreateEventRegistrationBuilder.builder()
                        .memberId(new MemberId(this.memberId))
                        .siCardNumber(this.siCardNumber != null ? new SiCardNumber(this.siCardNumber) : null)
                        .wantsToRentSi(this.wantsToRentSi)
                        .categoryId(this.categoryId != null ? new EventCategoryId(this.categoryId) : null)
                        .wantsSharedTransport(this.wantsSharedTransport)
                        .wantsSharedAccommodation(this.wantsSharedAccommodation)
                        .internalNote(RegistrationNote.ofNullable(this.internalNote))
                        .orisNote(RegistrationNote.ofNullable(this.orisNote))
                        .build());
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

    UUID getCategoryId() {
        return categoryId;
    }

    Instant getRegisteredAt() {
        return registeredAt;
    }

    boolean isWantsSharedTransport() {
        return wantsSharedTransport;
    }

    boolean isWantsSharedAccommodation() {
        return wantsSharedAccommodation;
    }

    boolean isWantsToRentSi() {
        return wantsToRentSi;
    }
}
