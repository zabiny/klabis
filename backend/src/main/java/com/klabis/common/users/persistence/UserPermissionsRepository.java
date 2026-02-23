package com.klabis.common.users.persistence;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserPermissions;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Optional;

/**
 * Repository interface for {@link UserPermissions} aggregate.
 * <p>
 * This repository follows DDD principles:
 * - Interface defined in authorization layer (authorization package)
 * - Implementation in infrastructure layer (persistence.jdbc package)
 * - Uses domain types (UserId, UserPermissions, Authority)
 * <p>
 * Lifecycle:
 * - UserPermissions are created lazily when first authority is granted
 * - Missing permissions are treated as empty (no authorities)
 * - Repository returns Optional.empty() for non-existent permissions
 * <p>
 * Design decisions:
 * - save() returns the saved aggregate for method chaining
 * - findById() returns Optional to handle lazy creation
 * - countUsersWithAuthority() supports admin lockout prevention
 *
 * @see UserPermissions
 * @see UserId
 * @see Authority
 */
@SecondaryPort
public interface UserPermissionsRepository {

    /**
     * Saves a UserPermissions aggregate.
     * <p>
     * Creates new record or updates existing record.
     *
     * @param permissions the permissions to save
     * @return the saved permissions (with generated ID if new)
     */
    UserPermissions save(UserPermissions permissions);

    /**
     * Finds permissions by user ID.
     * <p>
     * Returns Optional.empty() if user has no permissions record.
     * This supports lazy creation - permissions record created only when first authority is granted.
     *
     * @param userId the user ID
     * @return the permissions if found, empty otherwise
     */
    Optional<UserPermissions> findById(UserId userId);

    /**
     * Counts the number of active users with a specific authority.
     * <p>
     * Used for admin lockout prevention to ensure at least one user has MEMBERS:PERMISSIONS.
     * Only counts direct authorities (not group-based, since groups don't exist yet).
     * Only counts active users (account_status = 'ACTIVE').
     * <p>
     * Note: For future group authorization, this will still only count direct authorities
     * to prevent lockout when group memberships change.
     *
     * @param authority the authority to count
     * @return the number of active users with this direct authority
     */
    long countUsersWithAuthority(Authority authority);

    /**
     * Deletes all permissions for a user.
     * <p>
     * Used when user is deleted (though users currently cannot be deleted, only disabled).
     *
     * @param userId the user ID
     */
    void deleteByUserId(UserId userId);
}
