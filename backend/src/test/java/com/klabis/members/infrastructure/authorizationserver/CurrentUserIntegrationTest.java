package com.klabis.members.infrastructure.authorizationserver;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.members.CurrentUser;
import com.klabis.members.CurrentUserData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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
@Import(CurrentUserIntegrationTest.TestController.class)
class CurrentUserIntegrationTest {

    private static final String TEST_USER_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String TEST_MEMBER_ID = "123e4567-e89b-12d3-a456-426614174001";
    private static final String TEST_USERNAME = "123456";

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("should inject CurrentUserData with memberId in controller via @CurrentUser")
    @WithKlabisMockUser(username = TEST_USERNAME, userId = TEST_USER_ID, memberId = TEST_MEMBER_ID)
    @ParameterizedTest
    @ValueSource(strings = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
    void shouldInjectCurrentUserDataWithMemberIdInController(String acceptMediaType) throws Exception {
        mockMvc.perform(get("/api/currentUser").accept(acceptMediaType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID.toString()))
                .andExpect(jsonPath("$.memberId").value(TEST_MEMBER_ID.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
    @DisplayName("should inject CurrentUserData without memberId in controller via @CurrentUser")
    @WithKlabisMockUser(username = TEST_USERNAME, userId = TEST_USER_ID)
    void shouldInjectCurrentUserDataWithoutMemberIdInController(String acceptMediaType) throws Exception {
        mockMvc.perform(get("/api/currentUser").accept(acceptMediaType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID.toString()))
                .andExpect(jsonPath("$.memberId").isEmpty());
    }

    @RestController
    @SuppressWarnings("unused")
    static class TestController {

        record Response(UUID userId, UUID memberId) {
            public static Response from(CurrentUserData data) {
                return new Response(data.userId().uuid(), data.memberId() != null ? data.memberId().uuid() : null);
            }
        }

        @GetMapping(value = "/api/currentUser", produces =  {MediaTypes.HAL_FORMS_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
        EntityModel<Response> getCurrentUserData(@CurrentUser CurrentUserData userData) {
            return EntityModel.of(Response.from(userData));
        }
    }
}

