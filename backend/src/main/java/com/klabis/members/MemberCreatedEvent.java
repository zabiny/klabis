package com.klabis.members;

import com.klabis.members.domain.*;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain event published when a new Member is created.
 *
 * <p>This event can be used to trigger post-creation actions such as:
 * - Sending welcome emails with account activation links
 * - Creating audit log entries
 * - Notifying other bounded contexts
 * - Provisioning related resources
 *
 * <p>Domain events are immutable and represent facts that have already occurred.
 *
 * <p><b>Privacy Note:</b> This event contains personally identifiable information (PII).
 * Event listeners must handle this data in compliance with GDPR and data protection regulations.
 * Do not log the full event object.
 *
 * <p><b>Event Publishing:</b> Currently published synchronously within the transaction.
 * Future implementation will use the transactional outbox pattern (preferably with Spring Modulith)
 * to ensure reliable, exactly-once event delivery with guaranteed consistency.
 *
 * @see <a href="https://microservices.io/patterns/data/transactional-outbox.html">Transactional Outbox Pattern</a>
 * @see <a href="https://spring.io/projects/spring-modulith">Spring Modulith</a>
 */
@RecordBuilder
@DomainEvent
public record MemberCreatedEvent(
        UUID eventId,
        MemberId memberId,
        RegistrationNumber registrationNumber,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String nationality,
        Gender gender,
        Address address,
        EmailAddress email,
        PhoneNumber phone,
        GuardianInformation guardian,
        Instant occurredAt
) {

    /**
     * Creates a new MemberCreatedEvent with the current timestamp.
     *
     * @param memberId           the unique identifier of the created member
     * @param registrationNumber the member's registration number
     * @param firstName          the member's first name
     * @param lastName           the member's last name
     * @param dateOfBirth        the member's date of birth
     * @param nationality        the member's nationality
     * @param gender             the member's gender
     * @param address            the member's address
     * @param email              the member's email address
     * @param phone              the member's phone number
     * @param guardian           the guardian information (may be null for adults)
     */
    public MemberCreatedEvent(
            MemberId memberId,
            RegistrationNumber registrationNumber,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String nationality,
            Gender gender,
            Address address,
            EmailAddress email,
            PhoneNumber phone,
            GuardianInformation guardian) {
        this(
                UUID.randomUUID(),  // Generate unique event ID
                memberId,
                registrationNumber,
                firstName,
                lastName,
                dateOfBirth,
                nationality,
                gender,
                address,
                email,
                phone,
                guardian,
                Instant.now()  // Use current time
        );
    }

    /**
     * Compact constructor for validation.
     */
    public MemberCreatedEvent {
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(memberId, "Member ID is required");
        Objects.requireNonNull(registrationNumber, "Registration number is required");
        Objects.requireNonNull(firstName, "First name is required");
        Objects.requireNonNull(lastName, "Last name is required");
        Objects.requireNonNull(dateOfBirth, "Date of birth is required");
        Objects.requireNonNull(nationality, "Nationality is required");
        Objects.requireNonNull(gender, "Gender is required");
        Objects.requireNonNull(address, "Address is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
        // email, phone, guardian are nullable
    }

    /**
     * Factory method to create event from Member aggregate.
     *
     * @param member the member that was created
     * @return new MemberCreatedEvent
     */
    public static MemberCreatedEvent fromAggregate(Member member) {
        return new MemberCreatedEvent(
                member.getId(),
                member.getRegistrationNumber(),
                member.getFirstName(),
                member.getLastName(),
                member.getDateOfBirth(),
                member.getNationality(),
                member.getGender(),
                member.getAddress(),
                member.getEmail(),
                member.getPhone(),
                member.getGuardian()
        );
    }

    /**
     * Get member's email address as Optional.
     *
     * @return Optional containing member's email address, or empty if not provided
     */
    public Optional<EmailAddress> emailAsOptional() {
        return Optional.ofNullable(email);
    }

    /**
     * Get member's phone number as Optional.
     *
     * @return Optional containing member's phone number, or empty if not provided
     */
    public Optional<PhoneNumber> phoneAsOptional() {
        return Optional.ofNullable(phone);
    }

    /**
     * Check if member is a minor (has guardian).
     *
     * @return true if member has a guardian
     */
    public boolean isMinor() {
        return guardian != null;
    }

    /**
     * Get primary email for notifications.
     * Prefers member email if available, falls back to guardian email.
     *
     * @return primary email address as string, or null if none available
     */
    public String getPrimaryEmail() {
        if (email != null) {
            return email.value();
        }
        if (guardian != null) {
            return guardian.getEmail().value();
        }
        return null;
    }

    /**
     * Returns a string representation without PII for logging.
     * Does NOT include firstName, lastName, or other sensitive data to comply with GDPR.
     *
     * @return safe string representation for logs
     */
    @Override
    public String toString() {
        return "MemberCreatedEvent{" +
               "eventId=" + eventId +
               ", memberId=" + memberId +
               ", registrationNumber=" + registrationNumber +
               ", nationality='" + nationality + '\'' +
               ", isMinor=" + isMinor() +
               ", occurredAt=" + occurredAt +
               '}';
    }
}
