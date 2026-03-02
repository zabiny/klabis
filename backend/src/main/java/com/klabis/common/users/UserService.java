package com.klabis.common.users;

import com.klabis.common.users.domain.User;
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
     * Creates a new user who needs to set their password via the password setup flow.
     * <p>
     * A random placeholder password hash is generated internally. The user is created with
     * PENDING_ACTIVATION status. Publishes a UserCreatedEvent with the provided email so the
     * password setup handler can send the setup link to the correct address.
     *
     * @param username    the username (typically member registration number)
     * @param email       the email address for the password setup link
     * @param authorities the set of authorities to grant
     * @return the ID of the created user
     */
    UserId createUser(String username, String email, Set<Authority> authorities);

    /**
     * Creates a new user with an immediately active account using a pre-encoded password.
     * <p>
     * The user is created with ACTIVE status. No UserCreatedEvent is published, so no
     * password setup flow is triggered. Intended for bootstrap or administrative user creation.
     *
     * @param username     the username
     * @param passwordHash the BCrypt-hashed password
     * @param authorities  the set of authorities to grant
     * @return the ID of the created user
     */
    UserId createActiveUser(String username, String passwordHash, Set<Authority> authorities);

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

    /**
     * Suspends a user account by ID.
     * <p>
     * Sets the user's account status to SUSPENDED and enabled flag to false.
     * Suspended users cannot authenticate to the system. This operation is idempotent -
     * suspending an already suspended user completes successfully with no changes.
     * <p>
     * If the user account does not exist, the operation completes successfully without error
     * (graceful handling for missing accounts).
     *
     * @param userId the ID of the user to suspend
     */
    void suspendUser(UserId userId);

    /**
     * Resumes a suspended user account by ID.
     * <p>
     * Sets the user's account status to ACTIVE and enabled flag to true.
     * This restores the user's ability to authenticate. This operation is idempotent -
     * resuming an already active user completes successfully with no changes.
     * <p>
     * If the user account does not exist, the operation completes successfully without error
     * (graceful handling for missing accounts).
     *
     * @param userId the ID of the user to resume
     */
    void resumeUser(UserId userId);
}
