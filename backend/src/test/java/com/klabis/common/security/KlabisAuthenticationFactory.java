package com.klabis.common.security;

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

    public static KlabisJwtAuthenticationToken createAuthenticationToken(JwtParams jwtParams) {
        Jwt jwt = KlabisAuthenticationFactory.createKlabisToken(jwtParams);
        return new KlabisJwtAuthenticationToken(jwt,
                jwtParams.userId(),
                jwtParams.memberId(),
                jwtParams.grantedAuthorities());
    }



}
