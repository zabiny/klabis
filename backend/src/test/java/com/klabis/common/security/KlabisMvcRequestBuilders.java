package com.klabis.common.security;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Utility class that provides Spring MVC test request post‑processors for simulating
 * Klabis authentication and JWT tokens.
 *
 * <p>It offers a fluent {@link JwtParams} record to construct the minimal set of
 * claims required by the Klabis security infrastructure and two factory methods
 * that produce {@code RequestPostProcessor} instances usable with
 * {@code MockMvcRequestBuilders}.</p>
 */
public class KlabisMvcRequestBuilders {

    public record JwtParams(String userName, UserId userId, UUID memberId, Authority... authorities) {

        public static JwtParams jwtTokenParams(String userName, UUID userId) {
            return new JwtParams(userName, new UserId(userId), null);
        }

        public static JwtParams jwtTokenParams(String userName, UserId userId) {
            return new JwtParams(userName, userId, null);
        }

        public JwtParams withMemberId(UUID memberId) {
            return new JwtParams(userName, userId, memberId, authorities);
        }

        public JwtParams withAuthorities(Authority... authorities) {
            return new JwtParams(userName, userId, memberId, authorities);
        }

        public JwtParams addAuthority(Authority authority) {
            Authority[] newAuthorities = Stream.concat(Stream.of(authorities), Stream.of(authority))
                    .toArray(Authority[]::new);
            return new JwtParams(userName, userId, memberId, newAuthorities);
        }

        Collection<SimpleGrantedAuthority> grantedAuthorities() {
            return Stream.of(authorities).map(Authority::getValue).map(SimpleGrantedAuthority::new).toList();
        }

        Map<String, Object> toKlabisClaims() {
            Map<String, Object> claims = new HashMap<>();

            Collection<SimpleGrantedAuthority> convertedAuthorities = grantedAuthorities();

            claims.put(JwtClaimNames.SUB, userName);
            claims.put("user_id", userId.uuid().toString());
            claims.put("authorities", convertedAuthorities);

            if (memberId != null) {
                claims.put("member_id", memberId.toString());
            }
            return claims;
        }

    }

    public static RequestPostProcessor klabisAuthentication(JwtParams jwtParams) {
        return authentication(createAuthenticationToken(jwtParams));
    }

    private static KlabisJwtAuthenticationToken createAuthenticationToken(JwtParams jwtParams) {
        Jwt jwt = createToken(jwtParams);
        return new KlabisJwtAuthenticationToken(jwt, jwtParams.userId(), jwtParams.memberId(), jwtParams.grantedAuthorities());
    }

    private static Jwt createToken(JwtParams jwtParams) {
        Map<String, Object> claims = jwtParams.toKlabisClaims();

        return Jwt.withTokenValue("test-token")
                .header("alg", JwsAlgorithms.RS256)
                .claims(claimBuilder -> claimBuilder.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

}
