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
 * - memberId is required; siCardNumber is optional (the event's SI policy decides when it is needed)
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
            boolean wantsSharedAccommodation,
            RegistrationNote internalNote,
            RegistrationNote orisNote,
            boolean wantsToRentSi
    ) {}

    private final UUID id;
    @Association
    private final MemberId memberId;
    private final SiCardNumber siCardNumber;
    private final EventCategoryId categoryId;
    private final Instant registeredAt;
    private final boolean wantsSharedTransport;
    private final boolean wantsSharedAccommodation;
    private final RegistrationNote internalNote;
    private final RegistrationNote orisNote;
    private final boolean wantsToRentSi;
    private final boolean missingInOris;

    /**
     * Private constructor for creating EventRegistration instances.
     *
     * @param id           unique registration identifier
     * @param memberId     member's user ID
     * @param siCardNumber SI card number for this registration; absent when the member gave none
     * @param categoryId   selected race category id (may be null when event has no categories, or orphaned
     *                     when the category was later removed from the event)
     * @param registeredAt timestamp when registration was created
     */
    private EventRegistration(UUID id, MemberId memberId, SiCardNumber siCardNumber, EventCategoryId categoryId,
                              Instant registeredAt, boolean wantsSharedTransport, boolean wantsSharedAccommodation,
                              RegistrationNote internalNote, RegistrationNote orisNote,
                              boolean wantsToRentSi, boolean missingInOris) {
        this.id = id;
        this.memberId = memberId;
        this.siCardNumber = siCardNumber;
        this.categoryId = categoryId;
        this.registeredAt = registeredAt;
        this.wantsSharedTransport = wantsSharedTransport;
        this.wantsSharedAccommodation = wantsSharedAccommodation;
        this.internalNote = internalNote;
        this.orisNote = orisNote;
        this.wantsToRentSi = wantsToRentSi;
        this.missingInOris = missingInOris;
    }

    /**
     * Static factory method to create a new EventRegistration.
     * <p>
     * Generates a unique ID and sets the registration timestamp automatically.
     *
     * @param command creation command with memberId (required) and optional siCardNumber
     * @return new EventRegistration instance
     * @throws IllegalArgumentException if validation fails
     */
    public static EventRegistration create(CreateEventRegistration command) {
        return create(command, null);
    }

    /**
     * @param registeredAt registration time to keep (e.g. taken over from ORIS); {@code null} means now
     */
    public static EventRegistration create(CreateEventRegistration command, Instant registeredAt) {
        if (command.memberId() == null) {
            throw new IllegalArgumentException("memberId is required");
        }

        return new EventRegistration(
                UUID.randomUUID(),
                command.memberId(),
                command.siCardNumber(),
                command.categoryId(),
                registeredAt != null ? registeredAt : Instant.now(),
                command.wantsSharedTransport(),
                command.wantsSharedAccommodation(),
                command.internalNote(),
                command.orisNote(),
                command.wantsToRentSi(),
                false
        );
    }

    /**
     * Factory method for reconstructing EventRegistration from persistence layer.
     * <p>
     * This bypasses validation since the data was already validated when originally stored.
     */
    public static EventRegistration reconstruct(UUID id, Instant registeredAt, CreateEventRegistration data,
                                                boolean missingInOris) {
        return new EventRegistration(id, data.memberId(), data.siCardNumber(), data.categoryId(), registeredAt,
                data.wantsSharedTransport(), data.wantsSharedAccommodation(), data.internalNote(), data.orisNote(),
                data.wantsToRentSi(), missingInOris);
    }

    public EventRegistration withChanges(Event.EditRegistrationCommand changes) {
        return copy(changes.siCardNumber(), changes.categoryId(), changes.wantsSharedTransport(),
                changes.wantsSharedAccommodation(), changes.internalNote(), changes.orisNote(),
                changes.wantsToRentSi(), this.missingInOris);
    }

    /**
     * Takes values over from ORIS: clears the "missing in ORIS" mark, which {@link #withChanges} would keep.
     */
    EventRegistration withOrisValues(Event.EditRegistrationCommand changes) {
        return withChanges(changes).withMissingInOris(false);
    }

    EventRegistration markedMissingInOris() {
        return withMissingInOris(true);
    }

    private EventRegistration withMissingInOris(boolean missingInOris) {
        return copy(siCardNumber, categoryId, wantsSharedTransport, wantsSharedAccommodation, internalNote, orisNote,
                wantsToRentSi, missingInOris);
    }

    private EventRegistration copy(SiCardNumber siCardNumber, EventCategoryId categoryId, boolean wantsSharedTransport,
                                   boolean wantsSharedAccommodation, RegistrationNote internalNote,
                                   RegistrationNote orisNote, boolean wantsToRentSi, boolean missingInOris) {
        return new EventRegistration(id, memberId, siCardNumber, categoryId, registeredAt, wantsSharedTransport,
                wantsSharedAccommodation, internalNote, orisNote, wantsToRentSi, missingInOris);
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

    public RegistrationNote internalNote() {
        return internalNote;
    }

    public RegistrationNote orisNote() {
        return orisNote;
    }

    public boolean wantsToRentSi() {
        return wantsToRentSi;
    }

    public boolean missingInOris() {
        return missingInOris;
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
               ", wantsToRentSi=" + wantsToRentSi +
               '}';
    }
}
