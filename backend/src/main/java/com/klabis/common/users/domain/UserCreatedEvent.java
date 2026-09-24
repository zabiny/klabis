package com.klabis.common.users.domain;

import com.klabis.common.users.UserId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a new User is created.
 *
 * <p>This event can be used to trigger post-creation actions such as:
 * - Creating audit log entries
 * - Notifying other bounded contexts
 * - Provisioning related resources
 *
 * <p>Domain events are immutable and represent facts that have already occurred.
 *
 * <p><b>Event Publishing:</b> Published synchronously within the transaction using
 * Spring Modulith's transactional outbox pattern to ensure reliable, exactly-once event delivery
 * with guaranteed consistency.
 *
 * @see <a href="https://microservices.io/patterns/data/transactional-outbox.html">Transactional Outbox Pattern</a>
 * @see <a href="https://spring.io/projects/spring-modulith">Spring Modulith</a>
 */
@RecordBuilder
@DomainEvent
public record UserCreatedEvent(
        UUID eventId,
        UserId userId,
        String username,
        AccountStatus accountStatus,
        Instant occurredAt
) {

    /**
     * Canonical constructor with validation.
     * Creates a new UserCreatedEvent with explicit event ID and timestamp.
     *
     * @param eventId       unique identifier for this event
     * @param userId        the unique identifier of the created user
     * @param username      the user's username (registration number)
     * @param accountStatus the user's account status
     * @param occurredAt    the timestamp when this event occurred
     */
    public UserCreatedEvent {
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(username, "Username is required");
        Objects.requireNonNull(accountStatus, "Account status is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    /**
     * Creates a new UserCreatedEvent with generated event ID and current timestamp.
     * Useful for testing and default event creation.
     *
     * @param userId        the unique identifier of the created user
     * @param username      the user's username (registration number)
     * @param accountStatus the user's account status
     * @return new UserCreatedEvent with generated ID and current timestamp
     */
    public static UserCreatedEvent create(UserId userId, String username, AccountStatus accountStatus) {
        return new UserCreatedEvent(
                UUID.randomUUID(),
                userId,
                username,
                accountStatus,
                Instant.now()
        );
    }

    /**
     * Factory method to create event from User aggregate.
     *
     * @param user the user that was created
     * @return new UserCreatedEvent
     */
    public static UserCreatedEvent fromAggregate(User user) {
        return new UserCreatedEvent(
                UUID.randomUUID(),
                user.getId(),
                user.getUsername(),
                user.getAccountStatus(),
                Instant.now()
        );
    }

    /**
     * Check if user is pending activation.
     *
     * @return true if user account status is PENDING_ACTIVATION
     */
    public boolean isPendingActivation() {
        return accountStatus == AccountStatus.PENDING_ACTIVATION;
    }

    /**
     * Returns a string representation without PII for logging.
     * Does NOT include username or other sensitive data to comply with GDPR.
     *
     * @return safe string representation for logs
     */
    @Override
    public String toString() {
        return "UserCreatedEvent{" +
               "eventId=" + eventId +
               ", userId=" + userId +
               ", accountStatus=" + accountStatus +
               ", isPendingActivation=" + isPendingActivation() +
               ", occurredAt=" + occurredAt +
               '}';
    }
}
