package com.klabis.common.security;

import com.klabis.common.users.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for @CurrentUser annotation using Spring Boot test.
 * Tests the full flow from JWT authentication through CurrentUserArgumentResolver to controller.
 */
@ApplicationModuleTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CurrentUserIntegrationTest {

    private static final UUID TEST_USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID TEST_MEMBER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    private static final String TEST_USERNAME = "123456";

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private KlabisJwtAuthenticationToken createAuthenticationToken(UUID userId, UUID memberId) {
        var claimsBuilder = Map.<String, Object>of(
                JwtClaimNames.SUB, TEST_USERNAME,
                "user_id", userId.toString(),
                "authorities", List.of("MEMBERS:READ")
        );

        if (memberId != null) {
            var claimsWithMember = new HashMap<>(claimsBuilder);
            claimsWithMember.put("member_id", memberId.toString());
            return createToken(claimsWithMember);
        }

        return createToken(claimsBuilder);
    }

    private KlabisJwtAuthenticationToken createToken(Map<String, Object> claims) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", JwsAlgorithms.RS256)
                .claims(claimBuilder -> claimBuilder.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        if (claims.containsKey("member_id")) {
            UUID memberId = UUID.fromString((String) claims.get("member_id"));
            return new KlabisJwtAuthenticationToken(jwt, new UserId(TEST_USER_ID), memberId, List.of());
        } else {
            return new KlabisJwtAuthenticationToken(jwt, new UserId(TEST_USER_ID), null, List.of());
        }
    }

    @Test
    @DisplayName("should inject CurrentUserData with memberId in controller via @CurrentUser")
    void shouldInjectCurrentUserDataWithMemberIdInController() throws Exception {
        KlabisJwtAuthenticationToken token = createAuthenticationToken(TEST_USER_ID, TEST_MEMBER_ID);
        mockMvc.perform(get("/api")
                        .with(authentication(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID.toString()))
                .andExpect(jsonPath("$.memberId").value(TEST_MEMBER_ID.toString()));
    }

    @Test
    @DisplayName("should inject CurrentUserData without memberId in controller via @CurrentUser")
    void shouldInjectCurrentUserDataWithoutMemberIdInController() throws Exception {
        KlabisJwtAuthenticationToken token = createAuthenticationToken(TEST_USER_ID, null);
        mockMvc.perform(get("/api")
                        .with(authentication(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID.toString()))
                .andExpect(jsonPath("$.memberId").isEmpty());
    }

    @RestController
    @SuppressWarnings("unused")
    static class TestController {
        @GetMapping("/api")
        CurrentUserData getCurrentUserData(@CurrentUser CurrentUserData userData) {
            return userData;
        }
    }
}

