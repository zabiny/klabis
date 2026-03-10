package com.klabis.members;

import com.klabis.common.users.UserId;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.RegistrationNumber;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a suspended Member's membership is resumed.
 *
 * <p>This event can be used to trigger post-resume actions such as:
 * - Sending resume notification emails
 * - Creating audit log entries
 * - Notifying other bounded contexts (Finance, ORIS, CUS, Groups)
 * - Restoring access permissions
 *
 * <p><b>Privacy Note:</b> This event contains resume-related information.
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
@RecordBuilder
@DomainEvent
public record MemberResumedEvent(
        UUID eventId,
        MemberId memberId,
        RegistrationNumber registrationNumber,
        Instant resumedAt,
        UserId resumedBy
) {

    /**
     * Creates a new MemberResumedEvent with generated eventId.
     */
    public MemberResumedEvent(
            MemberId memberId,
            RegistrationNumber registrationNumber,
            Instant resumedAt,
            UserId resumedBy) {
        this(
                UUID.randomUUID(),
                memberId,
                registrationNumber,
                resumedAt,
                resumedBy
        );
    }

    /**
     * Compact constructor for validation.
     */
    public MemberResumedEvent {
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(memberId, "Member ID is required");
        Objects.requireNonNull(registrationNumber, "Registration number is required");
        Objects.requireNonNull(resumedAt, "Resumed at timestamp is required");
        Objects.requireNonNull(resumedBy, "Resumed by user ID is required");
    }

    public static MemberResumedEvent fromAggregate(Member member, Member.ResumeMembership command) {
        return new MemberResumedEvent(
                member.getId(),
                member.getRegistrationNumber(),
                Instant.now(),
                command.resumedBy()
        );
    }

    @Override
    public String toString() {
        return "MemberResumedEvent{" +
                "eventId=" + eventId +
                ", memberId=" + memberId +
                ", registrationNumber=" + registrationNumber +
                ", resumedAt=" + resumedAt +
                ", resumedBy=" + resumedBy +
                '}';
    }
}
