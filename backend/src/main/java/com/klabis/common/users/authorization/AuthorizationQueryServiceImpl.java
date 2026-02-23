package com.klabis.common.users.authorization;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserPermissions;
import com.klabis.common.users.persistence.UserPermissionsRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of {@link AuthorizationQueryService}.
 * <p>
 * Phase 1 implementation:
 * - Checks direct authorities only
 * - Loads UserPermissions from repository
 * - Treats missing permissions as empty (no authorities)
 * <p>
 * Phase 2 (future):
 * - Will integrate with member groups
 * - Will check group-based authorities
 * - Group admins receive authorities when accessing group members' data
 * <p>
 * Thread-safe: Service is stateless, all state in database
 * Transactional: Read-only transactions do not block
 */
@Service
@Transactional(readOnly = true)
public class AuthorizationQueryServiceImpl implements AuthorizationQueryService {

    private final UserPermissionsRepository permissionsRepo;

    public AuthorizationQueryServiceImpl(UserPermissionsRepository permissionsRepo) {
        this.permissionsRepo = permissionsRepo;
    }

    @Override
    public boolean checkAuthorization(AuthorizationContext context) {
        // Load UserPermissions for actor
        Optional<UserPermissions> permissions = permissionsRepo.findById(context.actor());

        // If no permissions record exists, treat as empty authorities
        if (permissions.isEmpty()) {
            return false;
        }

        // Check if actor has the required authority
        UserPermissions userPermissions = permissions.get();
        return userPermissions.hasDirectAuthority(context.requiredAuthority());
    }

    @Override
    public boolean hasAuthority(UserId userId, String authority) {
        try {
            Authority auth = Authority.fromString(authority);
            return hasAuthority(userId, auth);
        } catch (IllegalArgumentException e) {
            // Invalid authority string
            return false;
        }
    }

    /**
     * Helper method to check authority using the enum.
     *
     * @param userId    the user to check
     * @param authority the authority enum
     * @return true if user has the authority
     */
    private boolean hasAuthority(UserId userId, Authority authority) {
        Optional<UserPermissions> permissions = permissionsRepo.findById(userId);

        if (permissions.isEmpty()) {
            return false;
        }

        return permissions.get().hasDirectAuthority(authority);
    }
}
