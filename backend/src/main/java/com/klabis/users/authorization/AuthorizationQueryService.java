package com.klabis.users.authorization;

import com.klabis.users.UserId;
import com.klabis.users.UserPermissions;
import org.jmolecules.ddd.annotation.Service;

/**
 * Service for querying authorization decisions.
 * <p>
 * This service provides authorization checks based on context:
 * - Actor (who is making the request)
 * - Resource owner (whose data is being accessed)
 * - Required authority (what permission is needed)
 * <p>
 * Design:
 * - Phase 1: Checks direct authorities only (current implementation)
 * - Phase 2: Will check group-based authorities (future feature)
 * - Context-aware: Supports "who, whose data, what permission" model
 * <p>
 * Usage:
 * <pre>{@code
 * boolean authorized = authorizationQueryService.checkAuthorization(
 *     new AuthorizationContext(actorUserId, resourceOwnerUserId, Authority.MEMBERSHIP:VIEW)
 * );
 * }</pre>
 * <p>
 * The service is designed for extensibility - when groups feature is added,
 * group-based authorization checks can be included without breaking existing code.
 *
 * @see AuthorizationContext
 * @see UserPermissions
 */
@Service
public interface AuthorizationQueryService {

    /**
     * Checks if the actor is authorized based on the provided context.
     * <p>
     * Current implementation (Phase 1):
     * - Checks only direct authorities assigned to the actor
     * - Returns true if actor has the required authority
     * - Returns false otherwise
     * <p>
     * Future implementation (Phase 2):
     * - Will also check group-based authorities
     * - Group admins receive authorities when accessing group members' data
     * <p>
     * Design principle: Missing UserPermissions (not in database) is treated as empty authorities.
     * This means authorization check returns false for users with no permissions record.
     *
     * @param context the authorization context (actor, resourceOwner, requiredAuthority)
     * @return true if authorized, false otherwise
     */
    boolean checkAuthorization(AuthorizationContext context);

    /**
     * Checks if the given user has a specific authority.
     * <p>
     * Convenience method for simple authority checks without full context.
     * Use this when you only need to check "does user X have authority Y?"
     * <p>
     * This is equivalent to:
     * <pre>{@code
     * checkAuthorization(new AuthorizationContext(userId, userId, authority))
     * }</pre>
     *
     * @param userId    the user to check
     * @param authority the authority to check for
     * @return true if user has the authority, false otherwise
     */
    boolean hasAuthority(UserId userId, String authority);
}
