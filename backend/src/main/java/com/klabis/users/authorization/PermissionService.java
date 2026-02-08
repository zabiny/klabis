package com.klabis.users.authorization;

import com.klabis.users.*;
import com.klabis.users.persistence.UserPermissionsRepository;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Service for user permission management.
 * <p>
 * Manages user permissions using the {@link UserPermissions} aggregate,
 * separate from the {@code User} entity which handles authentication.
 * This separation enables authentication and authorization to evolve independently.
 *
 * <p><b>Business Rule:</b> At least one user with MEMBERS:PERMISSIONS authority must exist
 * to prevent admin lockout.
 *
 * @see UserPermissions
 * @see AuthorizationPolicy
 */
@Service
@PrimaryPort
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);
    private static final Authority MEMBERS_PERMISSIONS = Authority.MEMBERS_PERMISSIONS;

    private final UserPermissionsRepository permissionsRepository;

    public PermissionService(UserPermissionsRepository permissionsRepository) {
        this.permissionsRepository = permissionsRepository;
    }

    /**
     * Updates user permissions.
     * <p>
     * Enforces business rule: at least one user with MEMBERS:PERMISSIONS authority must exist.
     *
     * @param userId         the ID of the user to update
     * @param newAuthorities the new set of authorities to assign
     * @return the updated permissions
     * @throws UserNotFoundException                      if user not found
     * @throws CannotRemoveLastPermissionManagerException if this is the last user with MEMBERS:PERMISSIONS
     * @throws IllegalArgumentException                   if authorities are invalid (via AuthorityValidator)
     */
    @Transactional
    public UserPermissions updateUserPermissions(UserId userId, Set<Authority> newAuthorities) {
        log.debug("Updating permissions for user: {}", userId);

        // Validate authorities early (fail-fast before database operations)
        AuthorityValidator.validateAuthorityEnums(newAuthorities);

        // Load existing permissions or create new one
        UserPermissions permissions = permissionsRepository.findById(userId)
                .orElse(UserPermissions.empty(userId));

        // Check admin lockout prevention before making changes
        if (permissions.hasDirectAuthority(MEMBERS_PERMISSIONS) &&
            !newAuthorities.contains(MEMBERS_PERMISSIONS)) {
            // User currently has MEMBERS:PERMISSIONS and it's being removed
            long count = permissionsRepository.countUsersWithAuthority(MEMBERS_PERMISSIONS);
            AuthorizationPolicy.checkAdminLockoutPrevention(userId, MEMBERS_PERMISSIONS, count);
        }

        // Replace all authorities with new set
        permissions.replaceAuthorities(newAuthorities);

        // Save updated permissions
        UserPermissions savedPermissions = permissionsRepository.save(permissions);

        log.info("Updated permissions for user: {}", userId);

        return savedPermissions;
    }

    /**
     * Retrieves user permissions.
     *
     * @param userId the ID of the user
     * @return PermissionsResponse containing the user's authorities
     * @throws UserNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public PermissionsResponse getUserPermissions(UserId userId) {
        UserPermissions permissions = permissionsRepository.findById(userId)
                .orElse(UserPermissions.empty(userId));

        List<String> authorities = permissions.getDirectAuthorities().stream()
                .map(Authority::getValue)
                .toList();

        return new PermissionsResponse(userId, authorities);
    }
}
