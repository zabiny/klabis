package com.klabis.common.security;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Utility class that provides Spring MVC test request post‑processors for simulating
 * Klabis authentication and JWT tokens.
 */
public class KlabisMvcRequestBuilders {

    public static RequestPostProcessor klabisAuthentication(JwtParams jwtParams) {
        return authentication(KlabisAuthenticationFactory.createAuthenticationToken(jwtParams));
    }

}
