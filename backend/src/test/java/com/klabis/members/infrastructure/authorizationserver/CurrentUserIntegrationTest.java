package com.klabis.members.infrastructure.authorizationserver;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.UserService;
import com.klabis.members.ActingMember;
import com.klabis.members.ActingUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CurrentUserIntegrationTest.TestController.class)
@Import({CurrentUserArgumentResolver.class, MvcConfigurerMembers.class})
@MockitoBean(types = {UserService.class})
@WithPostprocessors
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

    @DisplayName("should inject CurrentUserData with memberId in controller via @ActingUser")
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
    @DisplayName("should inject CurrentUserData without memberId in controller via @ActingUser")
    @WithKlabisMockUser(username = TEST_USERNAME, userId = TEST_USER_ID)
    void shouldInjectCurrentUserDataWithoutMemberIdInController(String acceptMediaType) throws Exception {
        mockMvc.perform(get("/api/currentUser").accept(acceptMediaType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID.toString()))
                .andExpect(jsonPath("$.memberId").isEmpty());
    }

    @DisplayName("should inject MemberId in controller via @ActingMember when user has member profile")
    @WithKlabisMockUser(username = TEST_USERNAME, userId = TEST_USER_ID, memberId = TEST_MEMBER_ID)
    @ParameterizedTest
    @ValueSource(strings = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
    void shouldInjectMemberIdViaActingMember(String acceptMediaType) throws Exception {
        mockMvc.perform(get("/api/actingMember").accept(acceptMediaType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(TEST_MEMBER_ID.toString()));
    }

    @DisplayName("should return 403 via @ActingMember when user has no member profile")
    @WithKlabisMockUser(username = TEST_USERNAME, userId = TEST_USER_ID)
    @ParameterizedTest
    @ValueSource(strings = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
    void shouldReturn403WhenActingMemberHasNoMemberProfile(String acceptMediaType) throws Exception {
        mockMvc.perform(get("/api/actingMember").accept(acceptMediaType))
                .andExpect(status().isForbidden());
    }

    @MvcComponent
    @RestController
    @SuppressWarnings("unused")
    static class TestController {

        record Response(UUID userId, UUID memberId) {
            public static Response from(CurrentUserData data) {
                return new Response(data.userId().uuid(), data.memberId() != null ? data.memberId().uuid() : null);
            }
        }

        record MemberResponse(UUID memberId) {}

        @GetMapping(value = "/api/currentUser", produces = {MediaTypes.HAL_FORMS_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
        EntityModel<Response> getCurrentUserData(@ActingUser CurrentUserData userData) {
            return EntityModel.of(Response.from(userData));
        }

        @GetMapping(value = "/api/actingMember", produces = {MediaTypes.HAL_FORMS_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
        EntityModel<MemberResponse> getActingMember(@ActingMember MemberId memberId) {
            return EntityModel.of(new MemberResponse(memberId.uuid()));
        }
    }
}
