package com.klabis.users;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

/**
 * UserPermissions aggregate.
 * <p>
 * Manages direct authorities assigned to a user, separate from the User entity.
 * This separation enables authentication (User) and authorization (UserPermissions)
 * to evolve independently.
 * <p>
 * Business invariants:
 * - User must have a valid UserId
 * - Direct authorities set cannot be null (but can be empty)
 * - Granting authorities must respect admin lockout prevention
 * - Revoking authorities must respect admin lockout prevention
 * <p>
 * Authority Model:
 * - Only direct authorities are stored in this aggregate
 * - Future: group-based authorities will be computed at query time
 * - Authorities are strongly-typed using Authority enum
 * <p>
 * Persistence:
 * - Stored in user_permissions table
 * - Authorities serialized as JSON array string
 * - Lazy creation: permissions record created only when first authority is granted
 *
 * @see Authority
 * @see AuthorizationPolicy
 */
@AggregateRoot
public class UserPermissions {

    @Identity
    private final UserId userId;

    private Set<Authority> directAuthorities;

    // Optimistic locking
    private Long version;

    private boolean isNew = true;

    // ========== Command Records ==========

    /**
     * Command to grant an authority to a user.
     */
    public record GrantAuthority(Authority authority) {
    }

    /**
     * Command to revoke an authority from a user.
     */
    public record RevokeAuthority(Authority authority) {
    }

    /**
     * Command to replace all authorities with a new set.
     */
    public record ReplaceAuthorities(Set<Authority> authorities) {
    }

    // ========== Constructors ==========

    /**
     * Private constructor used by factory methods.
     *
     * @param userId            the unique user identifier
     * @param directAuthorities the set of direct authorities
     */
    private UserPermissions(UserId userId, Set<Authority> directAuthorities) {
        Assert.notNull(userId, "UserId must not be null");
        Assert.notNull(directAuthorities, "Direct authorities must not be null");

        this.userId = userId;
        this.directAuthorities = new HashSet<>(directAuthorities);
    }

    /**
     * Creates a new UserPermissions aggregate with the specified authorities.
     *
     * @param userId            the unique user identifier
     * @param directAuthorities the set of direct authorities
     * @return a new UserPermissions instance
     */
    public static UserPermissions create(UserId userId, Set<Authority> directAuthorities) {
        return new UserPermissions(userId, directAuthorities);
    }

    /**
     * Creates a new UserPermissions aggregate with no authorities.
     * <p>
     * Used when a user exists but has no direct authorities assigned.
     *
     * @param userId the unique user identifier
     * @return a new UserPermissions instance with empty authorities
     */
    public static UserPermissions empty(UserId userId) {
        return new UserPermissions(userId, Set.of());
    }

    /**
     * Gets the user ID.
     *
     * @return the user ID
     */
    public UserId getUserId() {
        return userId;
    }

    /**
     * Gets the direct authorities.
     * <p>
     * Returns an unmodifiable set to prevent external modification.
     *
     * @return an unmodifiable set of direct authorities
     */
    public Set<Authority> getDirectAuthorities() {
        return Set.copyOf(directAuthorities);
    }

    /**
     * Gets the version for optimistic locking.
     *
     * @return the version number
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the version for optimistic locking.
     * <p>
     * Used by persistence layer after save/update operations.
     *
     * @param version the version number
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Checks if this is a new (unsaved) UserPermissions instance.
     * <p>
     * Used by Spring Data JDBC via Persistable.isNew() to determine INSERT vs UPDATE.
     *
     * @return true if this is a new instance, false if already persisted
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * Marks this UserPermissions as persisted (no longer new).
     * <p>
     * Called by the repository after save operation.
     */
    public void markAsPersisted() {
        this.isNew = false;
    }

    /**
     * Checks if the user has a specific direct authority.
     *
     * @param authority the authority to check
     * @return true if the user has the authority, false otherwise
     */
    public boolean hasDirectAuthority(Authority authority) {
        return directAuthorities.contains(authority);
    }

    /**
     * Grants a direct authority to this user using a command.
     *
     * @param command the GrantAuthority command
     * @see AuthorizationPolicy#checkAdminLockoutPrevention
     */
    public void grantAuthority(GrantAuthority command) {
        grantAuthority(command.authority());
    }

    /**
     * Grants a direct authority to this user.
     * <p>
     * This method:
     * - Is idempotent (granting existing authority is a no-op)
     * - Creates a new set to maintain immutability semantics
     * - Does NOT enforce admin lockout prevention (caller must check via AuthorizationPolicy)
     * <p>
     * Admin lockout prevention must be checked before calling this method.
     *
     * @param authority the authority to grant
     * @see AuthorizationPolicy#checkAdminLockoutPrevention
     */
    public void grantAuthority(Authority authority) {
        Assert.notNull(authority, "Authority must not be null");

        // Idempotent: if authority already exists, no change needed
        if (directAuthorities.contains(authority)) {
            return;
        }

        // Create new set to maintain immutability
        Set<Authority> newAuthorities = new HashSet<>(this.directAuthorities);
        newAuthorities.add(authority);
        this.directAuthorities = newAuthorities;
    }

    /**
     * Revokes a direct authority from this user using a command.
     *
     * @param command the RevokeAuthority command
     * @see AuthorizationPolicy#checkAdminLockoutPrevention
     */
    public void revokeAuthority(RevokeAuthority command) {
        revokeAuthority(command.authority());
    }

    /**
     * Revokes a direct authority from this user.
     * <p>
     * This method:
     * - Is idempotent (revoking non-existent authority is a no-op)
     * - Creates a new set to maintain immutability semantics
     * - Does NOT enforce admin lockout prevention (caller must check via AuthorizationPolicy)
     * <p>
     * Admin lockout prevention must be checked before calling this method.
     *
     * @param authority the authority to revoke
     * @see AuthorizationPolicy#checkAdminLockoutPrevention
     */
    public void revokeAuthority(Authority authority) {
        Assert.notNull(authority, "Authority must not be null");

        // Idempotent: if authority doesn't exist, no change needed
        if (!directAuthorities.contains(authority)) {
            return;
        }

        // Create new set to maintain immutability
        Set<Authority> newAuthorities = new HashSet<>(this.directAuthorities);
        newAuthorities.remove(authority);
        this.directAuthorities = newAuthorities;
    }

    /**
     * Replaces all direct authorities with a new set using a command.
     *
     * @param command the ReplaceAuthorities command
     * @see AuthorizationPolicy#checkAdminLockoutPrevention
     */
    public void replaceAuthorities(ReplaceAuthorities command) {
        replaceAuthorities(command.authorities());
    }

    /**
     * Replaces all direct authorities with a new set.
     * <p>
     * This method does NOT enforce admin lockout prevention.
     * The caller must check via AuthorizationPolicy before calling this method.
     *
     * @param newAuthorities the new set of authorities
     * @see AuthorizationPolicy#checkAdminLockoutPrevention
     */
    public void replaceAuthorities(Set<Authority> newAuthorities) {
        Assert.notNull(newAuthorities, "New authorities must not be null");

        this.directAuthorities = new HashSet<>(newAuthorities);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserPermissions that = (UserPermissions) o;

        return userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }

    @Override
    public String toString() {
        return "UserPermissions{" +
               "userId=" + userId +
               ", directAuthorities=" + directAuthorities +
               '}';
    }
}
