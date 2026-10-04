package com.klabis.common.security;

import com.klabis.common.authorization.AuthorizationSnapshot;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.function.SingletonSupplier;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
 * The permission snapshot is not frozen into the access token: the token carries a memoized supplier that loads
 * it on the first authorization question. The resource server builds a new token for every request, so a
 * permission change applies to the next request of an already issued access token, while all decisions within
 * one request see the same snapshot.
 * <p>
 * Note: MemberId is stored as UUID (not MemberId type) to avoid module dependency
 * from common to members. Use MemberId.fromUuid() to convert in members module.
 */
public class KlabisJwtAuthenticationToken extends JwtAuthenticationToken {

    private static final Map<Authority, GrantedAuthority> GRANTED_AUTHORITIES = grantedAuthorities();

    private final UserId userId;
    private final UUID memberIdUuid;
    private final transient Supplier<AuthorizationSnapshot> snapshot;
    private final transient Supplier<List<GrantedAuthority>> authorities;

    public KlabisJwtAuthenticationToken(Jwt jwt, UserId userId, UUID memberIdUuid,
                                        Supplier<AuthorizationSnapshot> snapshotLoader) {
        super(jwt, List.of());
        this.userId = userId;
        this.memberIdUuid = memberIdUuid;
        this.snapshot = SingletonSupplier.of(snapshotLoader);
        this.authorities = SingletonSupplier.of(() -> snapshot.get().overAll().stream()
                .map(GRANTED_AUTHORITIES::get)
                .toList());
    }

    private static Map<Authority, GrantedAuthority> grantedAuthorities() {
        Map<Authority, GrantedAuthority> granted = new EnumMap<>(Authority.class);
        for (Authority authority : Authority.values()) {
            granted.put(authority, new SimpleGrantedAuthority(authority.getValue()));
        }
        return granted;
    }

    public AuthorizationSnapshot snapshot() {
        return snapshot.get();
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return authorities.get();
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

    /**
     * A member's id is its user id, and a non-member (a legal guardian) has a user id but no member profile,
     * so the id of the user identifies the owner in both cases.
     */
    public boolean isSelf(UUID id) {
        return id.equals(userId.uuid()) || id.equals(memberIdUuid);
    }

    public String getUsername() {
        return getToken().getSubject();
    }
}
