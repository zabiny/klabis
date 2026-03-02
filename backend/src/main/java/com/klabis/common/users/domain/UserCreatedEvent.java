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
public class UserCreatedEvent {

    private final UUID eventId;
    private final UserId userId;
    private final String username;
    private final AccountStatus accountStatus;
    private final Instant occurredAt;
    private final String email;  // Optional PII

    /**
     * Creates a new UserCreatedEvent with the current timestamp.
     *
     * @param userId        the unique identifier of the created user
     * @param username      the user's username (registration number)
     * @param accountStatus the user's account status
     */
    public UserCreatedEvent(
            UserId userId,
            String username,
            AccountStatus accountStatus) {
        this(
                UUID.randomUUID(),  // Generate unique event ID
                userId,
                username,
                accountStatus,
                Instant.now(),  // Use current time
                null  // No email by default
        );
    }

    /**
     * Creates a new UserCreatedEvent with explicit event ID and timestamp.
     * Useful for testing and event reconstruction.
     *
     * @param eventId       unique identifier for this event
     * @param userId        the unique identifier of the created user
     * @param username      the user's username (registration number)
     * @param accountStatus the user's account status
     * @param occurredAt    the timestamp when this event occurred
     */
    public UserCreatedEvent(
            UUID eventId,
            UserId userId,
            String username,
            AccountStatus accountStatus,
            Instant occurredAt) {
        this(
                eventId,
                userId,
                username,
                accountStatus,
                occurredAt,
                null  // No email by default
        );
    }

    /**
     * Creates a new UserCreatedEvent with explicit event ID, timestamp, and email.
     * Useful for testing and event reconstruction with PII.
     *
     * @param eventId       unique identifier for this event
     * @param userId        the unique identifier of the created user
     * @param username      the user's username (registration number)
     * @param accountStatus the user's account status
     * @param occurredAt    the timestamp when this event occurred
     * @param email         optional email for password setup coordination (may be null)
     */
    public UserCreatedEvent(
            UUID eventId,
            UserId userId,
            String username,
            AccountStatus accountStatus,
            Instant occurredAt,
            String email) {

        this.eventId = Objects.requireNonNull(eventId, "Event ID is required");
        this.userId = Objects.requireNonNull(userId, "User ID is required");
        this.username = Objects.requireNonNull(username, "Username is required");
        this.accountStatus = Objects.requireNonNull(accountStatus, "Account status is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
        this.email = email;  // Optional, may be null
    }

    /**
     * Factory method to create event from User aggregate.
     *
     * @param user the user that was created
     * @return new UserCreatedEvent without email
     */
    public static UserCreatedEvent fromUser(User user) {
        return new UserCreatedEvent(
                user.getId(),
                user.getUsername(),
                user.getAccountStatus()
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
    public static UserCreatedEvent fromUserWithEmail(User user, String email) {
        return new UserCreatedEvent(
                UUID.randomUUID(),  // Generate unique event ID
                user.getId(),
                user.getUsername(),
                user.getAccountStatus(),
                Instant.now(),  // Use current time
                email  // Include email for password setup
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

    public UserId getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    /**
     * Get the timestamp when this event occurred.
     *
     * @return event occurrence timestamp
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * Get the email as an Optional.
     * Email is optional PII used for password setup coordination during registration.
     *
     * @return Optional containing email if present, empty otherwise
     */
    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    /**
     * Check if user is pending activation.
     *
     * @return true if user account status is PENDING_ACTIVATION
     */
    public boolean isPendingActivation() {
        return accountStatus == AccountStatus.PENDING_ACTIVATION;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserCreatedEvent that = (UserCreatedEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
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
