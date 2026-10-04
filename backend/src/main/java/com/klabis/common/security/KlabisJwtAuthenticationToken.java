package com.klabis.common.security;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

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
 * Authorities are not frozen into the token: they are looked up through a supplier on every call, so a
 * permission change applies to the next request of an already issued access token.
 * <p>
 * Note: MemberId is stored as UUID (not MemberId type) to avoid module dependency
 * from common to members. Use MemberId.fromUuid() to convert in members module.
 */
public class KlabisJwtAuthenticationToken extends JwtAuthenticationToken {

    private final UserId userId;
    private final UUID memberIdUuid;
    private final transient Supplier<Collection<? extends GrantedAuthority>> authoritiesSupplier;

    public KlabisJwtAuthenticationToken(Jwt jwt, UserId userId, Collection<? extends GrantedAuthority> authorities) {
        this(jwt, userId, null, authorities);
    }

    public KlabisJwtAuthenticationToken(Jwt jwt, UserId userId, UUID memberIdUuid, Collection<? extends GrantedAuthority> authorities) {
        this(jwt, userId, memberIdUuid, fixed(authorities));
    }

    public KlabisJwtAuthenticationToken(Jwt jwt, UserId userId, UUID memberIdUuid,
                                        Supplier<Collection<? extends GrantedAuthority>> authoritiesSupplier) {
        super(jwt, List.of());
        this.userId = userId;
        this.memberIdUuid = memberIdUuid;
        this.authoritiesSupplier = authoritiesSupplier;
    }

    private static Supplier<Collection<? extends GrantedAuthority>> fixed(Collection<? extends GrantedAuthority> authorities) {
        List<GrantedAuthority> copy = List.copyOf(authorities);
        return () -> copy;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return List.copyOf(authoritiesSupplier.get());
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

    public boolean hasAuthority(Authority authority) {
        return getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(authority.getValue()));
    }

    public String getUsername() {
        return getToken().getSubject();
    }
}
