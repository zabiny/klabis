package com.klabis.common.security;

import com.klabis.common.users.UserId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Custom JWT authentication token containing UserId and optional MemberId.
 * <p>
 * Extends Spring Security's JwtAuthenticationToken to include strongly-typed
 * identifiers for the authenticated user, eliminating the need to parse
 * username or query database for MemberId.
 * <p>
 * UserId is extracted from JWT's user_id claim (mandatory).
 * MemberId is optional - not all Users have a Member profile (e.g., admins).
 * <p>
 * Note: MemberId is stored as UUID (not MemberId type) to avoid module dependency
 * from common to members. Use MemberId.fromUuid() to convert in members module.
 */
public class KlabisJwtAuthenticationToken extends JwtAuthenticationToken {

    private final UserId userId;
    private final UUID memberIdUuid;

    public KlabisJwtAuthenticationToken(Jwt jwt, UserId userId, Collection<? extends GrantedAuthority> authorities) {
        super(jwt, authorities);
        this.userId = userId;
        this.memberIdUuid = null;
    }

    public KlabisJwtAuthenticationToken(Jwt jwt, UserId userId, UUID memberIdUuid, Collection<? extends GrantedAuthority> authorities) {
        super(jwt, authorities);
        this.userId = userId;
        this.memberIdUuid = memberIdUuid;
    }

    public UserId getUserId() {
        return userId;
    }

    public Optional<UUID> getMemberIdUuid() {
        return Optional.ofNullable(memberIdUuid);
    }

    public boolean hasMemberProfile() {
        return memberIdUuid != null;
    }

    public String getUsername() {
        return getToken().getSubject();
    }
}
