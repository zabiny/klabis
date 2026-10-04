package com.klabis.common.security;

import com.klabis.common.authorization.AuthorizationSnapshot;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

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

    /**
     * Token whose snapshot holds the authorities of {@code jwtParams} over everything. Tests authenticate users
     * that have no stored permissions; the fixed snapshot stands in for what the database would say.
     */
    public static KlabisJwtAuthenticationToken createAuthenticationToken(JwtParams jwtParams) {
        return createAuthenticationToken(jwtParams, AuthorizationSnapshot.ofGrantedAuthorities(jwtParams.grantedAuthorities()));
    }

    public static KlabisJwtAuthenticationToken createAuthenticationToken(JwtParams jwtParams, AuthorizationSnapshot snapshot) {
        return new KlabisJwtAuthenticationToken(createKlabisToken(jwtParams),
                jwtParams.userId(),
                jwtParams.memberId(),
                () -> snapshot);
    }
}
