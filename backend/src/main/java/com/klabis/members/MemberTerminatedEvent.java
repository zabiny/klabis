package com.klabis.members;

import com.klabis.common.users.UserId;
import com.klabis.members.domain.DeactivationReason;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.RegistrationNumber;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a Member's membership is terminated.
 *
 * <p>This event can be used to trigger post-termination actions such as:
 * - Sending termination notification emails
 * - Creating audit log entries
 * - Notifying other bounded contexts (Finance, ORIS, CUS, Groups)
 * - Processing final financial settlements
 *
 * <p><b>User Account Suspension:</b> When a Member is terminated, the corresponding User account
 * is automatically suspended via {@link com.klabis.common.users.UserService#suspendUser(com.klabis.common.users.UserId)}.
 * This prevents the terminated member from authenticating to the system. The suspension happens in the same
 * transaction as Member termination, ensuring atomicity.
 *
 * <p><b>Privacy Note:</b> This event contains termination-related information.
 * Event listeners must handle this data in compliance with GDPR and data protection regulations.
 * Do not log the full event object with PII.
 *
 * <p><b>Event Publishing:</b> Currently published synchronously within the transaction.
 * Future implementation will use the transactional outbox pattern (preferably with Spring Modulith)
 * to ensure reliable, exactly-once event delivery with guaranteed consistency.
 *
 * @see <a href="https://microservices.io/patterns/data/transactional-outbox.html">Transactional Outbox Pattern</a>
 * @see <a href="https://spring.io/projects/spring-modulith">Spring Modulith</a>
 */
@DomainEvent
public class MemberTerminatedEvent {

    private final UUID eventId;
    private final UserId memberId;
    private final RegistrationNumber registrationNumber;
    private final DeactivationReason reason;
    private final Instant terminatedAt;
    private final UserId terminatedBy;
    private final String note;

    /**
     * Creates a new MemberTerminatedEvent with the current timestamp.
     *
     * @param memberId           the unique identifier of the terminated member
     * @param registrationNumber the member's registration number
     * @param reason             the reason for termination
     * @param terminatedAt       the timestamp when termination occurred
     * @param terminatedBy       the user who performed the termination
     * @param note               optional termination note
     */
    public MemberTerminatedEvent(
            UserId memberId,
            RegistrationNumber registrationNumber,
            DeactivationReason reason,
            Instant terminatedAt,
            UserId terminatedBy,
            String note) {
        this(
                UUID.randomUUID(),  // Generate unique event ID
                memberId,
                registrationNumber,
                reason,
                terminatedAt,
                terminatedBy,
                note
        );
    }

    /**
     * Creates a new MemberTerminatedEvent with explicit event ID.
     * Useful for testing and event reconstruction.
     *
     * @param eventId            unique identifier for this event
     * @param memberId           the unique identifier of the terminated member
     * @param registrationNumber the member's registration number
     * @param reason             the reason for termination
     * @param terminatedAt       the timestamp when termination occurred
     * @param terminatedBy       the user who performed the termination
     * @param note               optional termination note
     */
    public MemberTerminatedEvent(
            UUID eventId,
            UserId memberId,
            RegistrationNumber registrationNumber,
            DeactivationReason reason,
            Instant terminatedAt,
            UserId terminatedBy,
            String note) {

        this.eventId = Objects.requireNonNull(eventId, "Event ID is required");
        this.memberId = Objects.requireNonNull(memberId, "Member ID is required");
        this.registrationNumber = Objects.requireNonNull(registrationNumber, "Registration number is required");
        this.reason = Objects.requireNonNull(reason, "Termination reason is required");
        this.terminatedAt = Objects.requireNonNull(terminatedAt, "Terminated at timestamp is required");
        this.terminatedBy = Objects.requireNonNull(terminatedBy, "Terminated by user ID is required");
        this.note = note;
    }

    /**
     * Factory method to create event from Member aggregate and command.
     *
     * @param member  the member that was terminated
     * @param command the termination command
     * @return new MemberTerminatedEvent
     */
    public static MemberTerminatedEvent fromMember(Member member, Member.TerminateMembership command) {
        return new MemberTerminatedEvent(
                member.getId().toUserId(),  // change to MemberId
                member.getRegistrationNumber(),
                command.reason(),
                member.getDeactivatedAt(),
                command.terminatedBy(),
                command.note()
        );
    }

    // Getters

    /**
     * Get the unique event ID.
     * Used for idempotency checks and distributed tracing.
     *
     * @return unique event identifier
     */
    public UUID getEventId() {
        return eventId;
    }

    public UserId getMemberId() {
        return memberId;
    }

    public RegistrationNumber getRegistrationNumber() {
        return registrationNumber;
    }

    public DeactivationReason getReason() {
        return reason;
    }

    public Instant getTerminatedAt() {
        return terminatedAt;
    }

    public UserId getTerminatedBy() {
        return terminatedBy;
    }

    public String getNote() {
        return note;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberTerminatedEvent that = (MemberTerminatedEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    /**
     * Returns a string representation without PII for logging.
     * Does NOT include termination note or other potentially sensitive data.
     *
     * @return safe string representation for logs
     */
    @Override
    public String toString() {
        return "MemberTerminatedEvent{" +
                "eventId=" + eventId +
                ", memberId=" + memberId +
                ", registrationNumber=" + registrationNumber +
                ", reason=" + reason +
                ", terminatedAt=" + terminatedAt +
                ", terminatedBy=" + terminatedBy +
                '}';
    }
}
