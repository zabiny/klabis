package com.klabis.events.domain;

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
            String category
    ) {}

    private final UUID id;
    @Association
    private final MemberId memberId;
    private final SiCardNumber siCardNumber;
    private final String category;
    private final Instant registeredAt;

    /**
     * Private constructor for creating EventRegistration instances.
     *
     * @param id           unique registration identifier
     * @param memberId     member's user ID
     * @param siCardNumber SI card number for this registration
     * @param category     selected race category (may be null when event has no categories)
     * @param registeredAt timestamp when registration was created
     */
    private EventRegistration(UUID id, MemberId memberId, SiCardNumber siCardNumber, String category, Instant registeredAt) {
        this.id = id;
        this.memberId = memberId;
        this.siCardNumber = siCardNumber;
        this.category = category;
        this.registeredAt = registeredAt;
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
                command.category(),
                Instant.now()
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
    public static EventRegistration reconstruct(UUID id, MemberId memberId, SiCardNumber siCardNumber, String category, Instant registeredAt) {
        return new EventRegistration(id, memberId, siCardNumber, category, registeredAt);
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

    public String category() {
        return category;
    }

    public Instant registeredAt() {
        return registeredAt;
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
               ", category='" + category + '\'' +
               ", registeredAt=" + registeredAt +
               '}';
    }
}
