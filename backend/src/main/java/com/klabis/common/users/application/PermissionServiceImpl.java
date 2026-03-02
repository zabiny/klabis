package com.klabis.common.users.application;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.domain.AuthorityValidator;
import com.klabis.common.users.domain.AuthorizationPolicy;
import com.klabis.common.users.domain.UserPermissions;
import com.klabis.common.users.domain.UserPermissionsRepository;
import com.klabis.common.users.infrastructure.restapi.PermissionsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
class PermissionServiceImpl implements PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionServiceImpl.class);
    private static final Authority MEMBERS_PERMISSIONS = Authority.MEMBERS_PERMISSIONS;

    private final UserPermissionsRepository permissionsRepository;

    PermissionServiceImpl(UserPermissionsRepository permissionsRepository) {
        this.permissionsRepository = permissionsRepository;
    }

    @Override
    @Transactional
    public UserPermissions updateUserPermissions(UserId userId, Set<Authority> newAuthorities) {
        log.debug("Updating permissions for user: {}", userId);

        AuthorityValidator.validateAuthorityEnums(newAuthorities);

        UserPermissions permissions = permissionsRepository.findById(userId)
                .orElse(UserPermissions.empty(userId));

        if (permissions.hasDirectAuthority(MEMBERS_PERMISSIONS) &&
            !newAuthorities.contains(MEMBERS_PERMISSIONS)) {
            long count = permissionsRepository.countUsersWithAuthority(MEMBERS_PERMISSIONS);
            AuthorizationPolicy.checkAdminLockoutPrevention(userId, MEMBERS_PERMISSIONS, count);
        }

        permissions.replaceAuthorities(newAuthorities);

        UserPermissions savedPermissions = permissionsRepository.save(permissions);

        log.info("Updated permissions for user: {}", userId);

        return savedPermissions;
    }

    @Override
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
