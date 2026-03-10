package com.klabis.members;

import com.klabis.common.users.UserId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Internal domain event published when a member's birth number is accessed or modified.
 *
 * <p>Used for GDPR-compliant audit logging. Published by REST controllers and application services.
 * Handled by {@code BirthNumberAuditService} within the members module.
 *
 * <p>This event is intentionally intra-module — it stays in the members root package
 * for visibility within the module but is not consumed by other modules.
 */
@RecordBuilder
@DomainEvent
public record BirthNumberAccessedEvent(
        UUID eventId,
        UserId actingUserId,
        MemberId memberId,
        BirthNumberAction action,
        Instant occurredAt
) {

    public enum BirthNumberAction {
        VIEW_BIRTH_NUMBER,
        MODIFY_BIRTH_NUMBER
    }

    public BirthNumberAccessedEvent {
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(actingUserId, "Acting user ID is required");
        Objects.requireNonNull(memberId, "Member ID is required");
        Objects.requireNonNull(action, "Action is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    public static BirthNumberAccessedEvent viewed(UserId actingUserId, MemberId memberId) {
        return new BirthNumberAccessedEvent(
                UUID.randomUUID(),
                actingUserId,
                memberId,
                BirthNumberAction.VIEW_BIRTH_NUMBER,
                Instant.now()
        );
    }

    public static BirthNumberAccessedEvent modified(UserId actingUserId, MemberId memberId) {
        return new BirthNumberAccessedEvent(
                UUID.randomUUID(),
                actingUserId,
                memberId,
                BirthNumberAction.MODIFY_BIRTH_NUMBER,
                Instant.now()
        );
    }

    @Override
    public String toString() {
        return "BirthNumberAccessedEvent{" +
               "eventId=" + eventId +
               ", actingUserId=" + actingUserId +
               ", memberId=" + memberId +
               ", action=" + action +
               ", occurredAt=" + occurredAt +
               '}';
    }
}
