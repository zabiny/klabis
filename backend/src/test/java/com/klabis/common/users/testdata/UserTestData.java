package com.klabis.common.users.testdata;

import com.klabis.common.users.UserId;
import com.klabis.common.users.domain.AccountStatus;
import com.klabis.common.users.domain.User;

/**
 * Test data builders for User domain objects.
 * Provides pre-configured user instances for common test scenarios.
 * <p>
 * <b>Note:</b> Authorities are no longer part of the User entity.
 * They are managed separately in the UserPermissions aggregate.
 * Use {@code UserPermissionsTestData} for creating test data with authorities.
 */
public class UserTestData {

    // Common test password hash (bcrypt hash of "password123")
    public static final String DEFAULT_PASSWORD_HASH =
            "$2a$10$dXJ3SW6G7P50eS6xFXyO6u8KMNZfXH3d2S7yQv6uHEjj3.6xCWH46";

    /**
     * Creates an active admin user.
     */
    public static User adminUser() {
        return User.createdUser("admin", DEFAULT_PASSWORD_HASH);
    }

    /**
     * Creates an admin user with specified username.
     */
    public static User adminUser(String username) {
        return User.createdUser(username, DEFAULT_PASSWORD_HASH);
    }

    /**
     * Creates a regular member user.
     */
    public static User memberUser() {
        return User.createdUser("ZBM0101", DEFAULT_PASSWORD_HASH);
    }

    /**
     * Creates a member user with specified username.
     */
    public static User memberUser(String username) {
        return User.createdUser(username, DEFAULT_PASSWORD_HASH);
    }

    /**
     * Creates a pending activation user (inactive, requires password setup).
     */
    public static User pendingUser() {
        return User.createdUser("ZBM0201");
    }

    /**
     * Creates a pending user with specified username.
     */
    public static User pendingUser(String username) {
        return User.createdUser(username);
    }

    /**
     * Creates a user with custom properties using builder pattern.
     */
    public static UserBuilder custom() {
        return new UserBuilder();
    }

    /**
     * Fluent builder for custom user configurations.
     */
    public static class UserBuilder {
        private String username = "ZBM9999";
        private String passwordHash = DEFAULT_PASSWORD_HASH;
        private AccountStatus accountStatus = AccountStatus.ACTIVE;

        public UserBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public UserBuilder accountStatus(AccountStatus accountStatus) {
            this.accountStatus = accountStatus;
            return this;
        }

        public User build() {
            if (accountStatus == AccountStatus.PENDING_ACTIVATION) {
                return User.createdUser(username);
            }
            if (accountStatus == AccountStatus.ACTIVE) {
                return User.createdUser(username, passwordHash);
            }
            return User.reconstruct(new UserId(java.util.UUID.randomUUID()), username, passwordHash, accountStatus);
        }

        public User buildPending() {
            return User.createdUser(username);
        }
    }
}
