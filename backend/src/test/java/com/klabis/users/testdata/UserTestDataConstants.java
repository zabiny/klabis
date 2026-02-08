package com.klabis.users.testdata;

import com.klabis.users.Authority;

import java.util.Set;

/**
 * Shared constants for User test data.
 * <p>
 * This class contains commonly used values across multiple test files to eliminate duplication
 * and provide a single source of truth for test data constants.
 */
public final class UserTestDataConstants {

    /**
     * Default BCrypt password hash used in tests.
     * This is a valid BCrypt hash (not a real password).
     */
    public static final String DEFAULT_PASSWORD_HASH = "$2a$10$dXJ3SW6G7P50eS6xFXyO6u8KMNZfXH3d2S7yQv6uHEjj3.6xCWH46";

    /**
     * Default username for admin users in tests.
     */
    public static final String DEFAULT_ADMIN_USERNAME = "admin";

    /**
     * Default username for regular member users in tests.
     */
    public static final String DEFAULT_MEMBER_USERNAME = "ZBM0101";

    /**
     * Read-only authority set for regular members.
     * Members can only read member information.
     */
    public static final Set<Authority> READ_ONLY_AUTHORITIES = Set.of(Authority.MEMBERS_READ);

    /**
     * Full admin authority set.
     * Admins have all available permissions.
     */
    public static final Set<Authority> ADMIN_AUTHORITIES = Set.of(Authority.values());

    // Private constructor to prevent instantiation
    private UserTestDataConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
