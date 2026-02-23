package com.klabis.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.klabis.common.security.KlabisMvcRequestBuilders.JwtParams.jwtTokenParams;
import static com.klabis.common.security.KlabisMvcRequestBuilders.klabisAuthentication;
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

    @Test
    @DisplayName("should inject CurrentUserData with memberId in controller via @CurrentUser")
    void shouldInjectCurrentUserDataWithMemberIdInController() throws Exception {
        mockMvc.perform(get("/api")
                        .with(klabisAuthentication(jwtTokenParams(TEST_USERNAME, TEST_USER_ID).withMemberId(TEST_MEMBER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID.toString()))
                .andExpect(jsonPath("$.memberId").value(TEST_MEMBER_ID.toString()));
    }

    @Test
    @DisplayName("should inject CurrentUserData without memberId in controller via @CurrentUser")
    void shouldInjectCurrentUserDataWithoutMemberIdInController() throws Exception {
        mockMvc.perform(get("/api")
                        .with(klabisAuthentication(jwtTokenParams(TEST_USERNAME, TEST_USER_ID))))
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

