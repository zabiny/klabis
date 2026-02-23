package com.klabis.common.users.testdata;

import com.klabis.common.users.AccountStatus;
import com.klabis.common.users.User;
import com.klabis.common.users.UserId;

/**
 * Fluent builder for creating User test data.
 * <p>
 * This builder provides a convenient and readable way to create User instances in tests,
 * with sensible defaults and pre-configured factory methods for common user types.
 * <p>
 * Note: Authorities are no longer part of the User entity. They are managed separately
 * in the UserPermissions aggregate. This builder focuses only on User identity fields.
 * <p>
 * Usage examples:
 * <pre>{@code
 * // Create a default admin user
 * User admin = UserTestDataBuilder.anAdminUser().build();
 *
 * // Create a member user with custom username
 * User member = UserTestDataBuilder.aMemberUser()
 *         .username("ZBM0202")
 *         .build();
 *
 * // Create a pending activation user
 * User pending = UserTestDataBuilder.aPendingUser().build();
 *
 * // Create a user with custom configuration
 * User custom = UserTestDataBuilder.aMemberUser()
 *         .username("ZBM0303")
 *         .status(AccountStatus.SUSPENDED)
 *         .build();
 * }</pre>
 */
public class UserTestDataBuilder {

    private String username = UserTestDataConstants.DEFAULT_MEMBER_USERNAME;
    private String passwordHash = UserTestDataConstants.DEFAULT_PASSWORD_HASH;
    private AccountStatus status = AccountStatus.ACTIVE;

    private UserTestDataBuilder() {
    }

    /**
     * Creates a builder pre-configured for an admin user.
     * <p>
     * Default configuration:
     * <ul>
     *   <li>username: "admin"</li>
     *   <li>status: ACTIVE</li>
     * </ul>
     *
     * @return a builder configured for admin user
     */
    public static UserTestDataBuilder anAdminUser() {
        return new UserTestDataBuilder()
                .username(UserTestDataConstants.DEFAULT_ADMIN_USERNAME)
                .status(AccountStatus.ACTIVE);
    }

    /**
     * Creates a builder pre-configured for a regular member user.
     * <p>
     * Default configuration:
     * <ul>
     *   <li>username: "ZBM0101"</li>
     *   <li>status: ACTIVE</li>
     * </ul>
     *
     * @return a builder configured for member user
     */
    public static UserTestDataBuilder aMemberUser() {
        return new UserTestDataBuilder()
                .username(UserTestDataConstants.DEFAULT_MEMBER_USERNAME)
                .status(AccountStatus.ACTIVE);
    }

    /**
     * Creates a builder pre-configured for a pending activation user.
     * <p>
     * Default configuration:
     * <ul>
     *   <li>username: "ZBM0101"</li>
     *   <li>status: PENDING_ACTIVATION</li>
     * </ul>
     *
     * @return a builder configured for pending activation user
     */
    public static UserTestDataBuilder aPendingUser() {
        return new UserTestDataBuilder()
                .username(UserTestDataConstants.DEFAULT_MEMBER_USERNAME)
                .status(AccountStatus.PENDING_ACTIVATION);
    }

    /**
     * Sets the username for the user.
     *
     * @param username the username to set
     * @return this builder for method chaining
     */
    public UserTestDataBuilder username(String username) {
        this.username = username;
        return this;
    }

    /**
     * Sets the password hash for the user.
     *
     * @param passwordHash the password hash to set
     * @return this builder for method chaining
     */
    public UserTestDataBuilder passwordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    /**
     * Sets the account status for the user.
     *
     * @param status the account status to set
     * @return this builder for method chaining
     */
    public UserTestDataBuilder status(AccountStatus status) {
        this.status = status;
        return this;
    }

    /**
     * Builds and returns a User instance with the configured values.
     *
     * @return a new User instance
     */
    public User build() {
        if (status == AccountStatus.PENDING_ACTIVATION) {
            return User.createdUser(username);
        }
        if (status == AccountStatus.ACTIVE) {
            return User.createdUser(username, passwordHash);
        }
        return User.reconstruct(
                new UserId(java.util.UUID.randomUUID()),
                username,
                passwordHash,
                status,
                true,
                true,
                true,
                false
        );
    }
}
