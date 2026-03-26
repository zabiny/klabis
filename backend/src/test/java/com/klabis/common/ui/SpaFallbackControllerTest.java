package com.klabis.common.ui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaFallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldForwardRootToIndexHtml() throws Exception {
        mockMvc.perform(get("/")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void shouldForwardSingleLevelPathToIndexHtml() throws Exception {
        mockMvc.perform(get("/members")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void shouldForwardTwoLevelPathToIndexHtml() throws Exception {
        mockMvc.perform(get("/members/123")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void shouldForwardThreeLevelPathToIndexHtml() throws Exception {
        mockMvc.perform(get("/events/123/participants")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ})
    void shouldNotForwardApiRequests() throws Exception {
        // API requests should be handled by REST controllers, not SPA fallback
        // This verifies that /api/** paths are NOT matched by SPA fallback controller
        // We expect 401/403 (authentication/authorization error), NOT a forward to index.html
        mockMvc.perform(get("/api/members")
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(result -> {
                    // Verify it's NOT forwarded to index.html (which would be SPA fallback)
                    String forwardedUrl = result.getResponse().getForwardedUrl();
                    if (forwardedUrl != null && forwardedUrl.equals("/index.html")) {
                        throw new AssertionError("API request should NOT be forwarded to index.html");
                    }
                });
    }

    @Test
    void shouldNotForwardStaticFiles() throws Exception {
        // Static files with extensions should be handled by static resource handler
        // This test verifies the pattern exclusion works
        mockMvc.perform(get("/static/js/main.js")
                        .accept(MediaType.ALL))
                .andExpect(status().isNotFound()); // File doesn't exist, but pattern is correct
    }
}
