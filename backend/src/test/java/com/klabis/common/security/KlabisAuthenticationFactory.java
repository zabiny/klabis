package com.klabis.common.security;

import com.klabis.common.authorization.AuthorizationSnapshot;
import com.klabis.common.users.Authority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class KlabisAuthenticationFactory {

    public static Jwt createKlabisToken(JwtParams jwtParams) {
        Map<String, Object> claims = jwtParams.toKlabisClaims();

        return Jwt.withTokenValue("test-token")
                .header("alg", JwsAlgorithms.RS256)
                .claims(claimBuilder -> claimBuilder.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    public static KlabisJwtAuthenticationToken createAuthenticationToken(JwtParams jwtParams) {
        Jwt jwt = KlabisAuthenticationFactory.createKlabisToken(jwtParams);
        KlabisJwtAuthenticationToken token = new KlabisJwtAuthenticationToken(jwt,
                jwtParams.userId(),
                jwtParams.memberId(),
                jwtParams.grantedAuthorities());
        token.setDetails(snapshotOf(jwtParams.grantedAuthorities()));
        return token;
    }

    // Tests authenticate users that have no stored permissions; the fixed snapshot stands in for what the database would say
    private static AuthorizationSnapshot snapshotOf(Collection<? extends GrantedAuthority> granted) {
        Set<Authority> authorities = EnumSet.noneOf(Authority.class);
        for (GrantedAuthority authority : granted) {
            if (Authority.isKnownAuthority(authority.getAuthority())) {
                authorities.add(Authority.fromString(authority.getAuthority()));
            }
        }
        return AuthorizationSnapshot.of(authorities, Map.of());
    }



}
