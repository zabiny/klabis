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

    // Spring Security fields
    private boolean accountNonExpired = true;

    private boolean accountNonLocked = true;

    private boolean credentialsNonExpired = true;

    private boolean enabled = true;

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

    /**
     * Constructor for creating new User instances via factory methods.
     * <p>
     * This constructor is used by the static factory methods to ensure business invariants
     * are validated during construction.
     */
    private User(
            UserId id,
            String username,
            String passwordHash,
            AccountStatus accountStatus,
            boolean accountNonExpired,
            boolean accountNonLocked,
            boolean credentialsNonExpired,
            boolean enabled) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.accountStatus = accountStatus;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
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
                AccountStatus.PENDING_ACTIVATION,
                true,
                true,
                true,
                false
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
                AccountStatus.PENDING_ACTIVATION,
                true,
                true,
                true,
                false
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
                AccountStatus.ACTIVE,
                true,
                true,
                true,
                true
        );
    }

    /**
     * Reconstructs a User instance from persisted data (for repository load operations).
     * <p>
     * This method bypasses validation to allow reconstruction of users from the database.
     * Domain events are NOT registered during reconstruction.
     *
     * @param id                    user ID
     * @param username              username (registration number)
     * @param passwordHash          BCrypt-hashed password
     * @param accountStatus         account status
     * @param accountNonExpired     account non-expired flag
     * @param accountNonLocked      account non-locked flag
     * @param credentialsNonExpired credentials non-expired flag
     * @param enabled               enabled flag
     * @return User instance reconstructed from persisted data
     */
    public static User reconstruct(
            UserId id,
            String username,
            String passwordHash,
            AccountStatus accountStatus,
            boolean accountNonExpired,
            boolean accountNonLocked,
            boolean credentialsNonExpired,
            boolean enabled) {

        return new User(
                id,
                username,
                passwordHash,
                accountStatus,
                accountNonExpired,
                accountNonLocked,
                credentialsNonExpired,
                enabled
        );
    }

    /**
     * Activates this user account with a new password.
     *
     * <p>Returns a new User instance with ACTIVE status and the new password hash.
     * This method is called after the user successfully completes the password setup flow.
     *
     * @param newPasswordHash the BCrypt-hashed new password
     * @return new User instance with ACTIVE status and enabled=true
     */
    public User activateWithPassword(String newPasswordHash) {
        Objects.requireNonNull(newPasswordHash, "New password hash is required");

        User activated = new User(
                this.id,
                this.username,
                newPasswordHash,
                AccountStatus.ACTIVE,
                this.accountNonExpired,
                this.accountNonLocked,
                this.credentialsNonExpired,
                true
        );

        activated.updateAuditMetadata(this.getAuditMetadata());

        return activated;
    }

    /**
     * Suspends this user account.
     *
     * <p>Returns a new User instance with SUSPENDED status and enabled=false.
     * Suspended users cannot authenticate to the system (isAuthenticatable() returns false).
     *
     * @return new User instance with SUSPENDED status and enabled=false
     */
    public User suspend() {
        User suspended = new User(
                this.id,
                this.username,
                this.passwordHash,
                AccountStatus.SUSPENDED,
                this.accountNonExpired,
                this.accountNonLocked,
                this.credentialsNonExpired,
                false
        );

        suspended.updateAuditMetadata(this.getAuditMetadata());

        return suspended;
    }

    /**
     * Reactivates this suspended user account.
     *
     * <p>Returns a new User instance with ACTIVE status and enabled=true.
     * This restores the user's ability to authenticate.
     *
     * @return new User instance with ACTIVE status and enabled=true
     */
    public User reactivate() {
        User reactivated = new User(
                this.id,
                this.username,
                this.passwordHash,
                AccountStatus.ACTIVE,
                this.accountNonExpired,
                this.accountNonLocked,
                this.credentialsNonExpired,
                true
        );

        reactivated.updateAuditMetadata(this.getAuditMetadata());

        return reactivated;
    }

    private static void validateRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Password hash is required");
        }
    }

    /**
     * Check if user can authenticate.
     */
    public boolean isAuthenticatable() {
        return enabled
               && accountStatus == AccountStatus.ACTIVE
               && accountNonExpired
               && accountNonLocked
               && credentialsNonExpired;
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

    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "User{" +
               "id=" + id +
               ", username=" + username +
               ", accountStatus=" + accountStatus +
               ", enabled=" + enabled +
               '}';
    }
}
