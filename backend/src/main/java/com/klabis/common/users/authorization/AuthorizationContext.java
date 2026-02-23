package com.klabis.common.users.authorization;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserPermissions;
import org.springframework.util.Assert;

/**
 * AuthorizationContext value object.
 * <p>
 * Encapsulates the context required for making authorization decisions.
 * This context enables future group-based delegated authorization where
 * permissions may depend on whose data is being accessed.
 * <p>
 * Components:
 * - actor: Who is making the request (the authenticated user)
 * - resourceOwner: Whose data/resource is being accessed
 * - requiredAuthority: What authority is needed for the operation
 * <p>
 * Use cases:
 * - Direct authorization: actor has the required authority directly
 * - Future group authorization: actor receives authority via group membership
 * - Self-access scenarios: actor equals resourceOwner (no automatic rule, must have authority)
 * <p>
 * Example scenarios:
 * - User viewing their own profile: actor=userId1, resourceOwner=userId1, requiredAuthority=MEMBERSHIP:VIEW
 * - Admin viewing another user's profile: actor=adminId, resourceOwner=userId1, requiredAuthority=MEMBERSHIP:VIEW
 * - Group admin viewing group member's profile: actor=groupAdminId, resourceOwner=memberId, requiredAuthority=MEMBERSHIP:VIEW
 *
 * @see Authority
 * @see UserPermissions
 */
public record AuthorizationContext(
        UserId actor,
        UserId resourceOwner,
        Authority requiredAuthority
) {

    /**
     * Creates a new AuthorizationContext.
     *
     * @param actor             the user making the request
     * @param resourceOwner     the user whose data is being accessed
     * @param requiredAuthority the authority needed for the operation
     * @throws IllegalArgumentException if any parameter is null
     */
    public AuthorizationContext {
        Assert.notNull(actor, "Actor must not be null");
        Assert.notNull(resourceOwner, "ResourceOwner must not be null");
        Assert.notNull(requiredAuthority, "RequiredAuthority must not be null");
    }

    /**
     * Checks if this is a self-access context (actor accessing their own data).
     * <p>
     * Note: Self-access does NOT grant automatic permissions.
     * The user must still have the required authority.
     * This method is primarily useful for logging and audit purposes.
     *
     * @return true if actor equals resourceOwner, false otherwise
     */
    public boolean isSelfAccess() {
        return actor.equals(resourceOwner);
    }

    /**
     * Creates a self-access context (actor accessing their own data).
     *
     * @param actor             the user accessing their own data
     * @param requiredAuthority the authority needed
     * @return a new AuthorizationContext with actor=resourceOwner
     */
    public static AuthorizationContext selfAccess(UserId actor, Authority requiredAuthority) {
        return new AuthorizationContext(actor, actor, requiredAuthority);
    }

    /**
     * Creates a context for accessing another user's data.
     *
     * @param actor             the user making the request
     * @param resourceOwner     the user whose data is being accessed
     * @param requiredAuthority the authority needed
     * @return a new AuthorizationContext
     */
    public static AuthorizationContext crossAccess(UserId actor, UserId resourceOwner, Authority requiredAuthority) {
        return new AuthorizationContext(actor, resourceOwner, requiredAuthority);
    }
}
