package com.klabis.users;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Optional;
import java.util.Set;

/**
 * Service for user management operations.
 * <p>
 * Exposes business operations for creating and finding users.
 * This interface is at module root for inter-module communication.
 * <p>
 * Design principles:
 * - Business operations, not persistence details
 * - Transaction boundaries for multi-step operations
 * - Clean interface for other modules to depend on
 */
@PrimaryPort
public interface UserService {

    /**
     * Creates a new user with pending activation status and grants authorities.
     * <p>
     * Creates both User and UserPermissions in a single transaction.
     * The user is created with PENDING_ACTIVATION status and must complete
     * password setup to become ACTIVE.
     * <p>
     * Transaction behavior:
     * - User creation and permissions granting are atomic
     * - If permissions creation fails, user creation is rolled back
     * - Participates in caller's transaction (REQUIRED propagation)
     *
     * @param username     the username (typically member registration number)
     * @param passwordHash the BCrypt-hashed password
     * @param authorities  the set of authorities to grant
     * @return the ID of the created user
     * @throws IllegalArgumentException if username or passwordHash is invalid
     */
    UserId createUserPendingActivation(
            String username,
            String passwordHash,
            Set<Authority> authorities
    );

    /**
     * Creates a new user with pending activation status using builder pattern.
     * <p>
     * Creates both User and UserPermissions in a single transaction.
     * The user is created with PENDING_ACTIVATION status and must complete
     * password setup to become ACTIVE.
     * <p>
     * Optional email field supports password setup coordination during member registration.
     * If email is provided, UserCreatedEvent will include it for cross-module event handling.
     * <p>
     * Transaction behavior:
     * - User creation and permissions granting are atomic
     * - If permissions creation fails, user creation is rolled back
     * - Participates in caller's transaction (REQUIRED propagation)
     *
     * @param params the user creation parameters with optional email
     * @return the ID of the created user
     * @throws IllegalArgumentException if required parameters are invalid
     */
    UserId createUserPendingActivation(UserCreationParams params);

    /**
     * Finds a user by username.
     * <p>
     * Returns the complete User aggregate including credentials and account status.
     * Use this when you need to verify user existence or access user details.
     *
     * @param username the username to search for
     * @return optional containing the user if found, empty otherwise
     */
    Optional<User> findUserByUsername(String username);
}
