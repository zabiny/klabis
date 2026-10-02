package com.klabis.common.ui;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.CommonWebMvcTest;
import com.klabis.common.users.Authority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the wiring that carries the root link index from a plain RootModel payload through
 * HalResponseBodyAdvice into the RepresentationModelProcessor pipeline. If the payload stopped being
 * wrapped, the index would serialize as an empty object. Links contributed by other modules are
 * asserted in the tests of those modules.
 */
@CommonWebMvcTest
@DisplayName("RootController")
class RootControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Nested
    @DisplayName("GET /api")
    class GetRoot {

        @Test
        @WithKlabisMockUser(username = "admin", authorities = Authority.DEVELOPER)
        @DisplayName("wraps the plain payload so contributed links are rendered: admin link for a user with DEVELOPER authority")
        void shouldAddAdminLinkForDeveloper() throws Exception {
            mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.admin.href").value("/admin"));
        }

        @Test
        @WithKlabisMockUser(username = "admin", authorities = Authority.MEMBERS_MANAGE)
        @DisplayName("omits the admin link for a user without DEVELOPER authority")
        void shouldNotAddAdminLinkWithoutDeveloperAuthority() throws Exception {
            mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.admin").doesNotExist());
        }

        @Test
        @DisplayName("rejects unauthenticated request with 401")
        void shouldRejectUnauthenticatedRequest() throws Exception {
            mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                    .andExpect(status().isUnauthorized());
        }
    }
}
