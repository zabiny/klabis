package com.klabis.common.security;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.UserService;
import com.klabis.common.users.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that a suspended user is rejected with 403 Forbidden.
 * <p>
 * AccountStatusValidationFilter runs after BearerTokenAuthenticationFilter.
 * It checks the user account status from the database on every authenticated request
 * and returns HTTP 403 if the account is no longer active.
 * <p>
 * This test guards the behaviour that was previously duplicated in KlabisJwtAuthenticationConverter
 * before it was removed. The filter is now the sole enforcer of account-status checks.
 */
@WebMvcTest(controllers = AccountStatusValidationFilterTest.TestController.class)
@Import(AccountStatusValidationFilter.class)
@WithPostprocessors
@DisplayName("AccountStatusValidationFilter")
class AccountStatusValidationFilterTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserService userService;

    @MvcComponent
    @RestController
    static class TestController {
        @GetMapping(value = "/test/account-status", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        String get() {
            return "ok";
        }
    }

    @Test
    @DisplayName("should return 403 for suspended user")
    @WithKlabisMockUser(username = "ZBM0001")
    void shouldReturn403ForSuspendedUser() throws Exception {
        User suspendedUser = User.createdUser("ZBM0001", "hash").suspend();
        when(userService.findUserByUsername(anyString())).thenReturn(Optional.of(suspendedUser));

        mockMvc.perform(get("/test/account-status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("User account is no longer active"));
    }

    @Test
    @DisplayName("should pass through for active user")
    @WithKlabisMockUser(username = "ZBM0001")
    void shouldPassThroughForActiveUser() throws Exception {
        User activeUser = User.createdUser("ZBM0001", "hash");
        when(userService.findUserByUsername(anyString())).thenReturn(Optional.of(activeUser));

        mockMvc.perform(get("/test/account-status"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should pass through when user is not found in database")
    @WithKlabisMockUser(username = "ZBM0001")
    void shouldPassThroughWhenUserNotFound() throws Exception {
        when(userService.findUserByUsername(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/test/account-status"))
                .andExpect(status().isOk());
    }
}
