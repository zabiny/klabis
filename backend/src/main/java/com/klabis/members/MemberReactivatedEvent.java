package com.klabis.members;

import com.klabis.common.users.UserId;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.RegistrationNumber;
import com.klabis.members.MemberId;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a terminated Member's membership is reactivated.
 *
 * <p>This event can be used to trigger post-reactivation actions such as:
 * - Sending reactivation notification emails
 * - Creating audit log entries
 * - Notifying other bounded contexts (Finance, ORIS, CUS, Groups)
 * - Restoring access permissions
 *
 * <p><b>Privacy Note:</b> This event contains reactivation-related information.
 * Event listeners must handle this data in compliance with GDPR and data protection regulations.
 * Do not log the full event object with PII.
 *
 * <p><b>Event Publishing:</b> Published synchronously within the transaction.
 * Future implementation will use the transactional outbox pattern (preferably with Spring Modulith)
 * to ensure reliable, exactly-once event delivery with guaranteed consistency.
 *
 * @see <a href="https://microservices.io/patterns/data/transactional-outbox.html">Transactional Outbox Pattern</a>
 * @see <a href="https://spring.io/projects/spring-modulith">Spring Modulith</a>
 */
@DomainEvent
public class MemberReactivatedEvent {

    private final UUID eventId;
    private final MemberId memberId;
    private final RegistrationNumber registrationNumber;
    private final Instant reactivatedAt;
    private final UserId reactivatedBy;

    /**
     * Creates a new MemberReactivatedEvent with the current timestamp.
     *
     * @param memberId           the unique identifier of the reactivated member
     * @param registrationNumber the member's registration number
     * @param reactivatedAt      the timestamp when reactivation occurred
     * @param reactivatedBy      the user who performed the reactivation
     */
    public MemberReactivatedEvent(
            MemberId memberId,
            RegistrationNumber registrationNumber,
            Instant reactivatedAt,
            UserId reactivatedBy) {
        this(
                UUID.randomUUID(),  // Generate unique event ID
                memberId,
                registrationNumber,
                reactivatedAt,
                reactivatedBy
        );
    }

    /**
     * Creates a new MemberReactivatedEvent with explicit event ID.
     * Useful for testing and event reconstruction.
     *
     * @param eventId            unique identifier for this event
     * @param memberId           the unique identifier of the reactivated member
     * @param registrationNumber the member's registration number
     * @param reactivatedAt      the timestamp when reactivation occurred
     * @param reactivatedBy      the user who performed the reactivation
     */
    public MemberReactivatedEvent(
            UUID eventId,
            MemberId memberId,
            RegistrationNumber registrationNumber,
            Instant reactivatedAt,
            UserId reactivatedBy) {

        this.eventId = Objects.requireNonNull(eventId, "Event ID is required");
        this.memberId = Objects.requireNonNull(memberId, "Member ID is required");
        this.registrationNumber = Objects.requireNonNull(registrationNumber, "Registration number is required");
        this.reactivatedAt = Objects.requireNonNull(reactivatedAt, "Reactivated at timestamp is required");
        this.reactivatedBy = Objects.requireNonNull(reactivatedBy, "Reactivated by user ID is required");
    }

    /**
     * Factory method to create event from Member aggregate and command.
     *
     * @param member  the member that was reactivated
     * @param command the reactivation command
     * @return new MemberReactivatedEvent
     */
    public static MemberReactivatedEvent fromMember(Member member, Member.ReactivateMembership command) {
        return new MemberReactivatedEvent(
                member.getId(),
                member.getRegistrationNumber(),
                Instant.now(),
                command.reactivatedBy()
        );
    }

    // Getters

    /**
     * Used for idempotency checks and distributed tracing.
     */
    public UUID getEventId() {
        return eventId;
    }

    public MemberId getMemberId() {
        return memberId;
    }

    public RegistrationNumber getRegistrationNumber() {
        return registrationNumber;
    }

    public Instant getReactivatedAt() {
        return reactivatedAt;
    }

    public UserId getReactivatedBy() {
        return reactivatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberReactivatedEvent that = (MemberReactivatedEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    /**
     * Returns a string representation without PII for logging.
     *
     * @return safe string representation for logs
     */
    @Override
    public String toString() {
        return "MemberReactivatedEvent{" +
                "eventId=" + eventId +
                ", memberId=" + memberId +
                ", registrationNumber=" + registrationNumber +
                ", reactivatedAt=" + reactivatedAt +
                ", reactivatedBy=" + reactivatedBy +
                '}';
    }
}
