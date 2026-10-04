package com.klabis.common.authorization;

import com.klabis.common.security.KlabisJwtAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Supplies the permission snapshot of the current authentication. A user token memoizes its snapshot, so every
 * call within one request returns the same instance and all decisions of the request see one set of permissions.
 * Without an authenticated user (listeners, scheduled jobs) nothing is granted.
 */
@Component
public class AuthorizationSnapshotProvider {

    public AuthorizationSnapshot current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return AuthorizationSnapshot.empty();
        }
        if (authentication instanceof KlabisJwtAuthenticationToken userToken) {
            return userToken.snapshot();
        }
        return AuthorizationSnapshot.ofGrantedAuthorities(authentication.getAuthorities());
    }
}
