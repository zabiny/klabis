package com.klabis.users;

import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.users.persistence.jdbc.UserMemento;
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
 * @see com.klabis.users.UserPermissions
 * <p>
 * Persistence:
 * - This aggregate root is persisted via UserMemento using the Memento pattern
 * - ID is stored as UUID in database, exposed as UserId value object in domain
 * - Domain events are published via Spring Modulith's transactional outbox pattern
 * @see UserMemento
 */
@AggregateRoot
public class User extends KlabisAggregateRoot<UserId> {

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
     * Command to create a new active user.
     */
    public record CreateUser(String username, String passwordHash) {
    }

    /**
     * Command to create a new user pending activation with email for password setup.
     */
    public record CreatePendingActivationWithEmail(String username, String passwordHash, String email) {
    }

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
     * This constructor is used by the static factory methods (create, createPendingActivation)
     * to ensure business invariants are validated during construction.
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
     * Static factory method to create a new User using a command.
     *
     * @param command the CreateUser command
     * @return new User instance with ACTIVE status
     * @throws IllegalArgumentException if business rules are violated
     */
    public static User create(CreateUser command) {
        return create(command.username(), command.passwordHash());
    }

    /**
     * Static factory method to create a new User.
     *
     * @param username     username (registration number)
     * @param passwordHash BCrypt-hashed password
     * @return new User instance with ACTIVE status
     * @throws IllegalArgumentException if business rules are violated
     */
    public static User create(
            String username,
            String passwordHash) {

        Objects.requireNonNull(username, "User name is required");
        validateRequired(passwordHash, "Password hash");

        User user = new User(
                new UserId(UUID.randomUUID()),
                username,
                passwordHash,
                AccountStatus.ACTIVE,
                true, // accountNonExpired
                true, // accountNonLocked
                true, // credentialsNonExpired
                true  // enabled
        );

        // Register domain event
        user.registerEvent(UserCreatedEvent.fromUser(user));

        return user;
    }

    /**
     * Static factory method to create a new User with custom account settings.
     *
     * @param userName      registration number as string (username)
     * @param passwordHash  BCrypt-hashed password
     * @param accountStatus account status
     * @return new User instance
     * @throws IllegalArgumentException if business rules are violated
     */
    public static User create(
            String userName,
            String passwordHash,
            AccountStatus accountStatus) {

        Objects.requireNonNull(userName, "Registration number is required");
        validateRequired(passwordHash, "Password hash");
        Objects.requireNonNull(accountStatus, "Account status is required");

        User user = new User(
                new UserId(UUID.randomUUID()),
                userName,
                passwordHash,
                accountStatus,
                true, // accountNonExpired
                true, // accountNonLocked
                true, // credentialsNonExpired
                true  // enabled
        );

        // Register domain event
        user.registerEvent(UserCreatedEvent.fromUser(user));

        return user;
    }

    /**
     * Static factory method to create a new User with pending activation.
     *
     * <p>This is used during member registration when a password setup token will be generated.
     * The user account is created but cannot be used until the user sets their password via email link.
     *
     * @param userName     registration number as string (username)
     * @param passwordHash BCrypt-hashed password (temporary, will be replaced)
     * @return new User instance with PENDING_ACTIVATION status, not enabled
     * @throws IllegalArgumentException if business rules are violated
     */
    public static User createPendingActivation(
            String userName,
            String passwordHash) {

        Objects.requireNonNull(userName, "User name is required");
        validateRequired(passwordHash, "Password hash");

        User user = new User(
                new UserId(UUID.randomUUID()),
                userName,
                passwordHash,
                AccountStatus.PENDING_ACTIVATION,
                true, // accountNonExpired
                true, // accountNonLocked
                true, // credentialsNonExpired
                false // enabled = false until activated with password
        );

        // Register domain event
        user.registerEvent(UserCreatedEvent.fromUser(user));

        return user;
    }

    /**
     * Static factory method to create a new User with pending activation and email using a command.
     *
     * @param command the CreatePendingActivationWithEmail command
     * @return new User instance with PENDING_ACTIVATION status and UserCreatedEvent containing email
     * @throws IllegalArgumentException if business rules are violated
     */
    public static User createPendingActivationWithEmail(CreatePendingActivationWithEmail command) {
        return createPendingActivationWithEmail(
                command.username(),
                command.passwordHash(),
                command.email()
        );
    }

    /**
     * Static factory method to create a new User with pending activation and email for password setup.
     *
     * <p>This is used during member registration when an email will be sent for password setup.
     * The user account is created but cannot be used until the user sets their password via email link.
     * The email is included in the UserCreatedEvent for cross-module password setup coordination.
     *
     * @param userName     registration number as string (username)
     * @param passwordHash BCrypt-hashed password (temporary, will be replaced)
     * @param email        email address for password setup (PII from Member context)
     * @return new User instance with PENDING_ACTIVATION status and UserCreatedEvent containing email
     * @throws IllegalArgumentException if business rules are violated
     */
    public static User createPendingActivationWithEmail(
            String userName,
            String passwordHash,
            String email) {

        Objects.requireNonNull(userName, "User name is required");
        validateRequired(passwordHash, "Password hash");
        Objects.requireNonNull(email, "Email is required");

        User user = new User(
                new UserId(UUID.randomUUID()),
                userName,
                passwordHash,
                AccountStatus.PENDING_ACTIVATION,
                true, // accountNonExpired
                true, // accountNonLocked
                true, // credentialsNonExpired
                false // enabled = false until activated with password
        );

        // Register domain event with email
        user.registerEvent(UserCreatedEvent.fromUserWithEmail(user, email));

        return user;
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
     * Activates this user account with a new password using a command.
     *
     * @param command the ActivateWithPassword command
     * @return new User instance with ACTIVE status and enabled=true
     */
    public User activateWithPassword(ActivateWithPassword command) {
        return activateWithPassword(command.newPasswordHash());
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
                true // enabled = true after activation
        );

        // Copy audit metadata
        activated.updateAuditMetadata(this.getAuditMetadata());

        return activated;
    }

    private static void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
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
