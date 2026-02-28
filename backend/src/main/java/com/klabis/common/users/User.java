package com.klabis.common.users;

import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.common.users.persistence.jdbc.UserMemento;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.util.Objects;
import java.util.UUID;

/**
 * User aggregate root.
 * <p>
 * Represents a user account that can authenticate to the system.
 * Users are separate from Members (though typically linked via registrationNumber).
 * <p>
 * Business invariants:
 * - Registration number must be unique
 * - Password must be BCrypt-hashed
 * - Account status must be valid
 * <p>
 * Authority Model:
 * - Authorities are managed separately in UserPermissions aggregate
 * - This User entity focuses on identity only (credentials, account status)
 * - Uses strongly-typed Authority enum only (no role-based derivation)
 * - Roles are organizational labels only, do not grant authorities
 * - All authorities must be explicitly assigned and must match the Authority enum
 *
 * @see Authority
 * @see UserPermissions
 * <p>
 * Persistence:
 * - This aggregate root is persisted via UserMemento using the Memento pattern
 * - ID is stored as UUID in database, exposed as UserId value object in domain
 * - Domain events are published via Spring Modulith's transactional outbox pattern
 * @see UserMemento
 */
@AggregateRoot
public class User extends KlabisAggregateRoot<User, UserId> {

    @Identity
    private UserId id;

    private String username;

    private String passwordHash;

    private AccountStatus accountStatus;

    // ========== Command Records ==========

    /**
     * Command to activate a user with a new password.
     */
    public record ActivateWithPassword(String newPasswordHash) {
    }

    // ========== Constructors ==========

    /**
     * Default constructor.
     * Used for reconstruction from persistence.
     */
    protected User() {
    }

    private User(UserId id, String username, String passwordHash, AccountStatus accountStatus) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.accountStatus = accountStatus;
    }

    /**
     * Creates a new user who needs to set their password via a password setup flow.
     *
     * <p>A random placeholder password hash is generated internally so the account cannot
     * be used until the user completes the password setup. Publishes {@link UserCreatedEvent}
     * with {@link AccountStatus#PENDING_ACTIVATION} to trigger the password setup email.
     *
     * @param username registration number (username)
     * @return new User with PENDING_ACTIVATION status and a random placeholder password hash
     */
    public static User createdUser(String username) {
        Objects.requireNonNull(username, "Username is required");

        String placeholderHash = UUID.randomUUID().toString();

        User user = new User(
                new UserId(UUID.randomUUID()),
                username,
                placeholderHash,
                AccountStatus.PENDING_ACTIVATION
        );

        user.registerEvent(UserCreatedEvent.fromUser(user));

        return user;
    }

    /**
     * Creates a new user who needs to set their password via a password setup flow,
     * with an email included in the event for password setup coordination.
     *
     * <p>A random placeholder password hash is generated internally so the account cannot
     * be used until the user completes the password setup. Publishes {@link UserCreatedEvent}
     * with {@link AccountStatus#PENDING_ACTIVATION} and the email to trigger the password setup email.
     *
     * @param username registration number (username)
     * @param email    email address for password setup (PII from Member context)
     * @return new User with PENDING_ACTIVATION status and a random placeholder password hash
     */
    public static User createdUserWithEmail(String username, String email) {
        Objects.requireNonNull(username, "Username is required");
        Objects.requireNonNull(email, "Email is required");

        String placeholderHash = UUID.randomUUID().toString();

        User user = new User(
                new UserId(UUID.randomUUID()),
                username,
                placeholderHash,
                AccountStatus.PENDING_ACTIVATION
        );

        user.registerEvent(UserCreatedEvent.fromUserWithEmail(user, email));

        return user;
    }

    /**
     * Creates a new user with an immediately active account and a pre-encoded password.
     *
     * <p>No {@link UserCreatedEvent} is published because the account is ready for use
     * and no password setup flow should be triggered.
     *
     * @param username     registration number (username)
     * @param passwordHash BCrypt-hashed password
     * @return new User with ACTIVE status and the provided password hash
     */
    public static User createdUser(String username, String passwordHash) {
        Objects.requireNonNull(username, "Username is required");
        validateRequired(passwordHash);

        return new User(
                new UserId(UUID.randomUUID()),
                username,
                passwordHash,
                AccountStatus.ACTIVE
        );
    }

    /**
     * Reconstructs a User instance from persisted data (for repository load operations).
     * <p>
     * This method bypasses validation to allow reconstruction of users from the database.
     * Domain events are NOT registered during reconstruction.
     *
     * @param id            user ID
     * @param username      username (registration number)
     * @param passwordHash  BCrypt-hashed password
     * @param accountStatus account status
     * @return User instance reconstructed from persisted data
     */
    public static User reconstruct(UserId id, String username, String passwordHash, AccountStatus accountStatus) {
        return new User(id, username, passwordHash, accountStatus);
    }

    /**
     * Activates this user account with a new password.
     *
     * <p>Returns a new User instance with ACTIVE status and the new password hash.
     * This method is called after the user successfully completes the password setup flow.
     *
     * @param newPasswordHash the BCrypt-hashed new password
     * @return new User instance with ACTIVE status
     */
    public User activateWithPassword(String newPasswordHash) {
        Objects.requireNonNull(newPasswordHash, "New password hash is required");

        User activated = new User(this.id, this.username, newPasswordHash, AccountStatus.ACTIVE);

        activated.updateAuditMetadata(this.getAuditMetadata());

        return activated;
    }

    /**
     * Suspends this user account.
     *
     * <p>Returns a new User instance with SUSPENDED status.
     * Suspended users cannot authenticate to the system (isAuthenticatable() returns false).
     *
     * @return new User instance with SUSPENDED status
     */
    public User suspend() {
        User suspended = new User(this.id, this.username, this.passwordHash, AccountStatus.SUSPENDED);

        suspended.updateAuditMetadata(this.getAuditMetadata());

        return suspended;
    }

    /**
     * Resumes this suspended user account.
     *
     * <p>Returns a new User instance with ACTIVE status.
     * This restores the user's ability to authenticate.
     *
     * @return new User instance with ACTIVE status
     */
    public User resume() {
        User resumed = new User(this.id, this.username, this.passwordHash, AccountStatus.ACTIVE);

        resumed.updateAuditMetadata(this.getAuditMetadata());

        return resumed;
    }

    private static void validateRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Password hash is required");
        }
    }

    /**
     * Check if user can authenticate.
     * A user is authenticatable only when their account is ACTIVE.
     */
    public boolean isAuthenticatable() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    // Getters

    /**
     * Get user's ID as a UserId value object.
     *
     * @return UserId value object
     */
    @Override
    public UserId getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    @Override
    public String toString() {
        return "User{" +
               "id=" + id +
               ", username=" + username +
               ", accountStatus=" + accountStatus +
               '}';
    }
}
