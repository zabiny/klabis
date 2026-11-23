package com.klabis.common.security;

import com.klabis.common.authorization.AuthorizationSnapshot;
import com.klabis.common.authorization.TestSnapshots;
import com.klabis.common.users.Authority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Utility class that provides Spring MVC test request post‑processors for simulating
 * Klabis authentication and JWT tokens.
 */
public class KlabisMvcRequestBuilders {

    public static RequestPostProcessor klabisMemberWithSelfGrants(UUID memberId, Authority... authorities) {
        AuthorizationSnapshot snapshot = TestSnapshots.of(Set.of(authorities),
                TestSnapshots.selfGrants(memberId, true, false));
        return authentication(KlabisAuthenticationFactory.createAuthenticationToken(
                JwtParams.member(memberId).withAuthorities(authorities), snapshot));
    }

    public static RequestPostProcessor klabisAuthentication(JwtParams jwtParams) {
        return authentication(KlabisAuthenticationFactory.createAuthenticationToken(jwtParams));
    }

}
