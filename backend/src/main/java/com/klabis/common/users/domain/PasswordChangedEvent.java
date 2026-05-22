package com.klabis.common.users.domain;

import com.klabis.common.users.UserId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when an authenticated user changes their own password.
 */
@RecordBuilder
@DomainEvent
public record PasswordChangedEvent(
        UUID eventId,
        UserId userId,
        Instant occurredAt
) {

    public PasswordChangedEvent {
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    public static PasswordChangedEvent fromUser(User user) {
        return new PasswordChangedEvent(
                UUID.randomUUID(),
                user.getId(),
                Instant.now()
        );
    }

}
