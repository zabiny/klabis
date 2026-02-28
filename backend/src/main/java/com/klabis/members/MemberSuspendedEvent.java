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
 * Domain event published when a Member's membership is suspended.
 *
 * <p>This event can be used to trigger post-suspension actions such as:
 * - Sending suspension notification emails
 * - Creating audit log entries
 * - Notifying other bounded contexts (Finance, ORIS, CUS, Groups)
 * - Processing final financial settlements
 *
 * <p><b>User Account Suspension:</b> When a Member is suspended, the corresponding User account
 * is automatically suspended via {@link com.klabis.common.users.UserService#suspendUser(com.klabis.common.users.UserId)}.
 * This prevents the suspended member from authenticating to the system. The suspension happens in the same
 * transaction as Member suspension, ensuring atomicity.
 *
 * <p><b>Privacy Note:</b> This event contains suspension-related information.
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
public class MemberSuspendedEvent {

    private final UUID eventId;
    private final MemberId memberId;
    private final RegistrationNumber registrationNumber;
    private final DeactivationReason reason;
    private final Instant suspendedAt;
    private final UserId suspendedBy;
    private final String note;

    public MemberSuspendedEvent(
            MemberId memberId,
            RegistrationNumber registrationNumber,
            DeactivationReason reason,
            Instant suspendedAt,
            UserId suspendedBy,
            String note) {
        this(
                UUID.randomUUID(),
                memberId,
                registrationNumber,
                reason,
                suspendedAt,
                suspendedBy,
                note
        );
    }

    public MemberSuspendedEvent(
            UUID eventId,
            MemberId memberId,
            RegistrationNumber registrationNumber,
            DeactivationReason reason,
            Instant suspendedAt,
            UserId suspendedBy,
            String note) {

        this.eventId = Objects.requireNonNull(eventId, "Event ID is required");
        this.memberId = Objects.requireNonNull(memberId, "Member ID is required");
        this.registrationNumber = Objects.requireNonNull(registrationNumber, "Registration number is required");
        this.reason = Objects.requireNonNull(reason, "Suspension reason is required");
        this.suspendedAt = Objects.requireNonNull(suspendedAt, "Suspended at timestamp is required");
        this.suspendedBy = Objects.requireNonNull(suspendedBy, "Suspended by user ID is required");
        this.note = note;
    }

    public static MemberSuspendedEvent fromMember(Member member, Member.SuspendMembership command) {
        return new MemberSuspendedEvent(
                member.getId(),
                member.getRegistrationNumber(),
                command.reason(),
                member.getSuspendedAt(),
                command.suspendedBy(),
                command.note()
        );
    }

    public UUID getEventId() {
        return eventId;
    }

    public MemberId getMemberId() {
        return memberId;
    }

    public RegistrationNumber getRegistrationNumber() {
        return registrationNumber;
    }

    public DeactivationReason getReason() {
        return reason;
    }

    public Instant getSuspendedAt() {
        return suspendedAt;
    }

    public UserId getSuspendedBy() {
        return suspendedBy;
    }

    public String getNote() {
        return note;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberSuspendedEvent that = (MemberSuspendedEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "MemberSuspendedEvent{" +
                "eventId=" + eventId +
                ", memberId=" + memberId +
                ", registrationNumber=" + registrationNumber +
                ", reason=" + reason +
                ", suspendedAt=" + suspendedAt +
                ", suspendedBy=" + suspendedBy +
                '}';
    }
}
