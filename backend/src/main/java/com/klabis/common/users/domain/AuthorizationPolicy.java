package com.klabis.common.users.domain;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;

/**
 * AuthorizationPolicy - business rules for authorization decisions.
 * <p>
 * This class encapsulates authorization business rules that must be enforced
 * when granting or revoking authorities. It provides:
 * - Admin lockout prevention
 * - Authority scope validation (global vs context-specific)
 * - Group authorization validation (future)
 * <p>
 * Design principles:
 * - Stateless: all context passed as method parameters
 * - Testable: easy to unit test without dependencies
 * - Clear error messages: throws exceptions with descriptive messages
 * <p>
 * Usage:
 * - Call checkAdminLockoutPrevention() before revoking MEMBERS:PERMISSIONS
 * - Call checkGlobalAuthorityNotGrantedViaGroup() before group authority grants
 *
 * @see Authority
 * @see UserPermissions
 */
public final class AuthorizationPolicy {

    private AuthorizationPolicy() {
        // Utility class - prevent instantiation
    }

    /**
     * Checks if revoking an authority would violate admin lockout prevention.
     * <p>
     * Admin lockout prevention ensures that at least one active user always has
     * the MEMBERS:PERMISSIONS authority. This prevents the system from having
     * no users who can manage permissions.
     * <p>
     * Business rules:
     * - If revoking MEMBERS:PERMISSIONS from a user who has it
     * - And only one active user has MEMBERS:PERMISSIONS
     * - Then throw exception
     * <p>
     * Note: This check does NOT verify if the target user actually has the authority.
     * It only checks if revoking it would cause lockout.
     *
     * @param targetUserId      the user from whom authority would be revoked
     * @param authorityToRevoke the authority being revoked
     * @param currentAdminCount the number of active users with MEMBERS:PERMISSIONS
     * @throws AdminLockoutException if revoking would leave zero permission managers
     */
    public static void checkAdminLockoutPrevention(
            UserId targetUserId,
            Authority authorityToRevoke,
            long currentAdminCount
    ) {
        // Only check admin lockout for MEMBERS:PERMISSIONS authority
        if (!Authority.MEMBERS_PERMISSIONS.equals(authorityToRevoke)) {
            return;
        }

        // If only one admin exists, prevent revoking
        if (currentAdminCount <= 1) {
            throw new AdminLockoutException(
                    String.format(
                            "Cannot revoke MEMBERS:PERMISSIONS from user %s. " +
                            "This would leave the system with zero permission managers. " +
                            "Grant MEMBERS:PERMISSIONS to another user first.",
                            targetUserId.uuid()
                    )
            );
        }
    }

    /**
     * Checks if a global authority is being granted via group mechanism.
     * <p>
     * Global authorities (MEMBERS:PERMISSIONS, SYSTEM:ADMIN) cannot be granted
     * via groups to prevent privilege escalation. They must be granted directly.
     * <p>
     * Business rules:
     * - Global authorities cannot be granted via groups
     * - Context-specific authorities can be granted via groups
     * <p>
     * Note: This is designed for future group-based authorization.
     * Currently, no group authorization exists, so this validates future design.
     *
     * @param authority the authority being granted via group
     * @throws IllegalArgumentException if authority is global and grant is via group
     */
    public static void checkGlobalAuthorityNotGrantedViaGroup(Authority authority) {
        if (authority.getScope() == Authority.Scope.GLOBAL) {
            throw new IllegalArgumentException(
                    String.format(
                            "Global authority %s cannot be granted via groups. " +
                            "Global authorities must be granted directly to users. " +
                            "Authority scope: %s",
                            authority.getValue(),
                            authority.getScope()
                    )
            );
        }
    }

    /**
     * Exception thrown when admin lockout prevention is violated.
     * <p>
     * This exception indicates that an operation would leave the system
     * with zero users having MEMBERS:PERMISSIONS authority.
     */
    public static class AdminLockoutException extends RuntimeException {
        public AdminLockoutException(String message) {
            super(message);
        }
    }
}
