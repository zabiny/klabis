package com.klabis.common.ui;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
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

@WebMvcTest(controllers = DashboardController.class)
@Import(HalFormsSupport.class)
@DisplayName("DashboardController")
@WithPostprocessors
class DashboardControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Nested
    @DisplayName("GET /api/dashboard")
    class GetDashboard {

        @Test
        @WithKlabisMockUser(username = "anyAuthenticatedUser")
        @DisplayName("returns 200 with self link for authenticated user")
        void shouldReturn200WithSelfLinkForAuthenticatedUser() throws Exception {
            mockMvc.perform(get("/api/dashboard").accept(MediaTypes.HAL_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/dashboard"));
        }

        @Test
        @DisplayName("rejects unauthenticated request with 401")
        void shouldRejectUnauthenticatedRequest() throws Exception {
            mockMvc.perform(get("/api/dashboard").accept(MediaTypes.HAL_JSON_VALUE))
                    .andExpect(status().isUnauthorized());
        }
    }
}
