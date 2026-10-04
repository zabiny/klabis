package com.klabis.common.authorization;

import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.application.PermissionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads the snapshot lazily on the first authorization question of a request, so requests that never ask pay
 * nothing, and keeps it for the rest of the request.
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.INTERFACES)
class RequestScopedAuthorizationSnapshotProvider implements AuthorizationSnapshotProvider {

    private final PermissionService permissionService;
    private final ObjectProvider<RelationshipSource> relationshipSources;
    private AuthorizationSnapshot snapshot;

    RequestScopedAuthorizationSnapshotProvider(PermissionService permissionService,
                                               ObjectProvider<RelationshipSource> relationshipSources) {
        this.permissionService = permissionService;
        this.relationshipSources = relationshipSources;
    }

    @Override
    public synchronized AuthorizationSnapshot current() {
        if (snapshot == null) {
            snapshot = load(SecurityContextHolder.getContext().getAuthentication());
        }
        return snapshot;
    }

    private AuthorizationSnapshot load(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return AuthorizationSnapshot.empty();
        }
        if (authentication instanceof KlabisJwtAuthenticationToken userToken) {
            return loadForUser(userToken.getUserId());
        }
        return AuthorizationSnapshot.of(knownAuthorities(authentication), Map.of());
    }

    private AuthorizationSnapshot loadForUser(UserId userId) {
        Set<Authority> overAll = Authority.withStandard(permissionService.getUserPermissions(userId).getDirectAuthorities());
        List<Map<Authority, Set<TargetRef>>> sourceGrants = relationshipSources.orderedStream()
                .map(source -> source.grantsOf(userId))
                .toList();
        return AuthorizationSnapshot.fromSources(overAll, sourceGrants);
    }

    private Set<Authority> knownAuthorities(Authentication authentication) {
        Set<Authority> known = EnumSet.noneOf(Authority.class);
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            if (Authority.isKnownAuthority(granted.getAuthority())) {
                known.add(Authority.fromString(granted.getAuthority()));
            }
        }
        return known;
    }
}
