package com.klabis.common.users.domain;

import com.klabis.common.users.UserId;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain event published when a new User is created.
 *
 * <p>This event can be used to trigger post-creation actions such as:
 * - Sending password setup emails
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
 * <p><b>Event Publishing:</b> Published synchronously within the transaction using
 * Spring Modulith's transactional outbox pattern to ensure reliable, exactly-once event delivery
 * with guaranteed consistency.
 *
 * @see <a href="https://microservices.io/patterns/data/transactional-outbox.html">Transactional Outbox Pattern</a>
 * @see <a href="https://spring.io/projects/spring-modulith">Spring Modulith</a>
 */
@DomainEvent
public record UserCreatedEvent(
        UUID eventId,
        UserId userId,
        String username,
        AccountStatus accountStatus,
        Instant occurredAt,
        String rawEmail  // Optional PII
) {

    /**
     * Canonical constructor with validation.
     * Creates a new UserCreatedEvent with explicit event ID, timestamp, and email.
     *
     * @param eventId       unique identifier for this event
     * @param userId        the unique identifier of the created user
     * @param username      the user's username (registration number)
     * @param accountStatus the user's account status
     * @param occurredAt    the timestamp when this event occurred
     * @param rawEmail      optional email for password setup coordination (may be null)
     */
    public UserCreatedEvent {
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(username, "Username is required");
        Objects.requireNonNull(accountStatus, "Account status is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
        // rawEmail is optional, may be null
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
                Instant.now(),
                null
        );
    }

    /**
     * Factory method to create event from User aggregate.
     *
     * @param user the user that was created
     * @return new UserCreatedEvent without email
     */
    public static UserCreatedEvent fromAggregate(User user) {
        return new UserCreatedEvent(
                UUID.randomUUID(),
                user.getId(),
                user.getUsername(),
                user.getAccountStatus(),
                Instant.now(),
                null
        );
    }

    /**
     * Factory method to create event from User aggregate with email.
     * Makes PII flow explicit in code.
     *
     * @param user  the user that was created
     * @param email the email address (PII from Member context)
     * @return new UserCreatedEvent with email
     */
    public static UserCreatedEvent fromAggregateWithEmail(User user, String email) {
        return new UserCreatedEvent(
                UUID.randomUUID(),
                user.getId(),
                user.getUsername(),
                user.getAccountStatus(),
                Instant.now(),
                email
        );
    }

    /**
     * Get the email as an Optional.
     * Email is optional PII used for password setup coordination during registration.
     *
     * @return Optional containing email if present, empty otherwise
     */
    public Optional<String> email() {
        return Optional.ofNullable(rawEmail);
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
