package com.klabis.events.domain;

import com.klabis.events.EventCategoryId;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a member's registration for an event.
 * <p>
 * This represents the association between a member and an event,
 * capturing the SI card number they will use for the event.
 * <p>
 * Business rules:
 * - Each registration has a unique ID
 * - Both memberId and siCardNumber are required
 * - Registration timestamp is automatically set at creation
 * - Equality is based solely on registration ID
 */
@ValueObject
public class EventRegistration {

    @RecordBuilder
    public record CreateEventRegistration(
            MemberId memberId,
            SiCardNumber siCardNumber,
            EventCategoryId categoryId,
            boolean wantsSharedTransport,
            boolean wantsSharedAccommodation
    ) {}

    private final UUID id;
    @Association
    private final MemberId memberId;
    private final SiCardNumber siCardNumber;
    private final EventCategoryId categoryId;
    private final Instant registeredAt;
    private final boolean wantsSharedTransport;
    private final boolean wantsSharedAccommodation;

    /**
     * Private constructor for creating EventRegistration instances.
     *
     * @param id           unique registration identifier
     * @param memberId     member's user ID
     * @param siCardNumber SI card number for this registration
     * @param categoryId   selected race category id (may be null when event has no categories, or orphaned
     *                     when the category was later removed from the event)
     * @param registeredAt timestamp when registration was created
     */
    private EventRegistration(UUID id, MemberId memberId, SiCardNumber siCardNumber, EventCategoryId categoryId,
                              Instant registeredAt, boolean wantsSharedTransport, boolean wantsSharedAccommodation) {
        this.id = id;
        this.memberId = memberId;
        this.siCardNumber = siCardNumber;
        this.categoryId = categoryId;
        this.registeredAt = registeredAt;
        this.wantsSharedTransport = wantsSharedTransport;
        this.wantsSharedAccommodation = wantsSharedAccommodation;
    }

    /**
     * Static factory method to create a new EventRegistration.
     * <p>
     * Generates a unique ID and sets the registration timestamp automatically.
     *
     * @param command creation command with memberId and siCardNumber (both required)
     * @return new EventRegistration instance
     * @throws IllegalArgumentException if validation fails
     */
    public static EventRegistration create(CreateEventRegistration command) {
        if (command.memberId() == null) {
            throw new IllegalArgumentException("memberId is required");
        }
        if (command.siCardNumber() == null) {
            throw new IllegalArgumentException("siCardNumber is required");
        }

        return new EventRegistration(
                UUID.randomUUID(),
                command.memberId(),
                command.siCardNumber(),
                command.categoryId(),
                Instant.now(),
                command.wantsSharedTransport(),
                command.wantsSharedAccommodation()
        );
    }

    /**
     * Factory method for reconstructing EventRegistration from persistence layer.
     * <p>
     * This bypasses validation since the data was already validated when originally stored.
     *
     * @param id           registration's unique identifier
     * @param memberId     member's user ID
     * @param siCardNumber SI card number
     * @param registeredAt registration timestamp
     * @return reconstructed EventRegistration instance
     */
    public static EventRegistration reconstruct(UUID id, MemberId memberId, SiCardNumber siCardNumber, EventCategoryId categoryId,
                                               Instant registeredAt) {
        return reconstruct(id, memberId, siCardNumber, categoryId, registeredAt, false, false);
    }

    public static EventRegistration reconstruct(UUID id, MemberId memberId, SiCardNumber siCardNumber, EventCategoryId categoryId,
                                               Instant registeredAt, boolean wantsSharedTransport, boolean wantsSharedAccommodation) {
        return new EventRegistration(id, memberId, siCardNumber, categoryId, registeredAt,
                wantsSharedTransport, wantsSharedAccommodation);
    }

    public EventRegistration withChanges(SiCardNumber newSiCard, EventCategoryId newCategoryId) {
        return withChanges(newSiCard, newCategoryId, this.wantsSharedTransport, this.wantsSharedAccommodation);
    }

    public EventRegistration withChanges(SiCardNumber newSiCard, EventCategoryId newCategoryId,
                                         boolean newWantsSharedTransport, boolean newWantsSharedAccommodation) {
        return new EventRegistration(this.id, this.memberId, newSiCard, newCategoryId, this.registeredAt,
                newWantsSharedTransport, newWantsSharedAccommodation);
    }

    // ========== Getters ==========

    public UUID id() {
        return id;
    }

    public MemberId memberId() {
        return memberId;
    }

    public SiCardNumber siCardNumber() {
        return siCardNumber;
    }

    public EventCategoryId categoryId() {
        return categoryId;
    }

    public Instant registeredAt() {
        return registeredAt;
    }

    public boolean wantsSharedTransport() {
        return wantsSharedTransport;
    }

    public boolean wantsSharedAccommodation() {
        return wantsSharedAccommodation;
    }

    // ========== Object Methods ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventRegistration that = (EventRegistration) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "EventRegistration{" +
               "id=" + id +
               ", memberId=" + memberId +
               ", siCardNumber=" + siCardNumber +
               ", categoryId=" + categoryId +
               ", registeredAt=" + registeredAt +
               ", wantsSharedTransport=" + wantsSharedTransport +
               ", wantsSharedAccommodation=" + wantsSharedAccommodation +
               '}';
    }
}
