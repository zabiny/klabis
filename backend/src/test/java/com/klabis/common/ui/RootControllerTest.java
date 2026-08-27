package com.klabis.common.ui;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.users.Authority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the wiring that carries the root link index from a plain RootModel payload through
 * HalResponseBodyAdvice into the RepresentationModelProcessor pipeline. Nine modules contribute
 * links here; if the payload stopped being wrapped, the index would serialize as an empty object
 * and every module's unit test on its own processor would still pass.
 */
@WebMvcTest(controllers = RootController.class)
@Import(HalFormsSupport.class)
@DisplayName("RootController")
@WithPostprocessors
class RootControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Nested
    @DisplayName("GET /api")
    class GetRoot {

        @Test
        @WithKlabisMockUser(username = "anyAuthenticatedUser")
        @DisplayName("wraps the plain payload so contributed links are rendered")
        void shouldRenderLinksForAuthenticatedUser() throws Exception {
            mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links").exists());
        }

        @Test
        @WithKlabisMockUser(username = "admin", authorities = Authority.DEVELOPER)
        @DisplayName("adds the admin link for a user with DEVELOPER authority")
        void shouldAddAdminLinkForDeveloper() throws Exception {
            mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.admin.href").value("/sandplace"));
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
