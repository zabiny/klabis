package com.klabis.calendar.infrastructure.security;

import com.klabis.common.users.UserId;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * Authentication token for iCal feed access via a raw PAT query parameter.
 * <p>
 * Created unauthenticated by {@link IcalTokenAuthenticationFilter} when a {@code ?token=}
 * query parameter is present on an {@code /ical/**} request. After successful validation by
 * {@link IcalTokenAuthenticationProvider}, the token is replaced with an authenticated instance
 * carrying the resolved {@link UserId} as principal (accessible via {@link #getPrincipal()}).
 */
class IcalTokenAuthenticationToken extends AbstractAuthenticationToken {

    private final String rawToken;
    private final UserId userId;

    /** Unauthenticated — created by the filter before validation. */
    IcalTokenAuthenticationToken(String rawToken) {
        super(List.of());
        this.rawToken = rawToken;
        this.userId = null;
        setAuthenticated(false);
    }

    /** Authenticated — created by the provider after successful validation. */
    IcalTokenAuthenticationToken(UserId userId, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.rawToken = null;
        this.userId = userId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return rawToken;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    String getRawToken() {
        return rawToken;
    }
}
