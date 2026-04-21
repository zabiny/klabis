package com.klabis.common.security;

import com.klabis.common.users.UserId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.UUID;

/**
 * Converts JWT tokens into KlabisJwtAuthenticationToken with strongly-typed UserId.
 * <p>
 * Extracts user_id claim from JWT and creates KlabisJwtAuthenticationToken.
 * Authorities are extracted using the standard JwtGrantedAuthoritiesConverter.
 * Account-status validation (suspended/deactivated users) is handled separately
 * by {@link AccountStatusValidationFilter} after JWT authentication.
 * <p>
 * JWT Claims Structure:
 * - sub: username (registration number) - standard JWT subject claim
 * - user_id: UUID of the user - custom claim for type-safe access
 * - member_id: UUID of the member (optional) - custom claim if user has Member profile
 * - authorities: array of authority strings - for authorization
 */
public class KlabisJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter authoritiesConverter;

    public KlabisJwtAuthenticationConverter() {
        this.authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        this.authoritiesConverter.setAuthoritiesClaimName("authorities");
        this.authoritiesConverter.setAuthorityPrefix("");
    }

    /**
     * Sets the authorities claim name in the JWT.
     * Default is "authorities".
     *
     * @param authoritiesClaimName the claim name containing authorities
     */
    public void setAuthoritiesClaimName(String authoritiesClaimName) {
        this.authoritiesConverter.setAuthoritiesClaimName(authoritiesClaimName);
    }

    /**
     * Sets the authority prefix.
     * Default is empty string (no prefix).
     *
     * @param authorityPrefix the prefix to add to authorities
     */
    public void setAuthorityPrefix(String authorityPrefix) {
        this.authoritiesConverter.setAuthorityPrefix(authorityPrefix);
    }

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        Collection<? extends GrantedAuthority> authorities = authoritiesConverter.convert(jwt);

        String userIdClaim = jwt.getClaim("user_id");
        if (userIdClaim == null) {
            return new JwtAuthenticationToken(jwt, authorities);
        }

        UserId userId = extractUserId(jwt);
        UUID memberIdUuid = extractMemberIdUuid(jwt);

        return new KlabisJwtAuthenticationToken(jwt, userId, memberIdUuid, authorities);
    }

    /**
     * Extracts UserId from the user_id claim in JWT.
     *
     * @param jwt the JWT token
     * @return the UserId
     * @throws IllegalArgumentException if user_id claim is missing or invalid
     */
    private UserId extractUserId(Jwt jwt) {
        String userIdClaim = jwt.getClaim("user_id");
        if (userIdClaim == null || userIdClaim.isBlank()) {
            throw new IllegalArgumentException("JWT missing required 'user_id' claim");
        }

        try {
            UUID uuid = UUID.fromString(userIdClaim);
            return new UserId(uuid);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("JWT 'user_id' claim is not a valid UUID: " + userIdClaim, e);
        }
    }

    /**
     * Extracts optional MemberId as UUID from the member_id claim in JWT.
     *
     * @param jwt the JWT token
     * @return the UUID, or null if claim is not present
     */
    private UUID extractMemberIdUuid(Jwt jwt) {
        String memberIdClaim = jwt.getClaim("member_id");
        if (memberIdClaim == null || memberIdClaim.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(memberIdClaim);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("JWT 'member_id' claim is not a valid UUID: " + memberIdClaim, e);
        }
    }
}
