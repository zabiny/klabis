package com.klabis.common;

import com.klabis.common.authorization.AuthorizationSnapshot;
import com.klabis.common.authorization.AuthorizationSnapshotProvider;
import com.klabis.common.users.Authority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Test replacement of the database-backed snapshot provider: uses the snapshot {@link WithKlabisMockUser}
 * attached to the authentication, and for any other authentication the known authorities it carries.
 */
public class FixedAuthorizationSnapshotProvider implements AuthorizationSnapshotProvider {

    @Override
    public AuthorizationSnapshot current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return AuthorizationSnapshot.empty();
        }
        if (authentication.getDetails() instanceof AuthorizationSnapshot fixed) {
            return fixed;
        }
        Set<Authority> authorities = EnumSet.noneOf(Authority.class);
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            if (Authority.isKnownAuthority(granted.getAuthority())) {
                authorities.add(Authority.fromString(granted.getAuthority()));
            }
        }
        return AuthorizationSnapshot.of(authorities, Map.of());
    }
}
