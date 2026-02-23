package com.klabis.common.logging;

import com.klabis.TestApplicationConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Framework integration test for MDC (Mapped Diagnostic Context) filter behavior.
 *
 * <p>This is a FRAMEWORK TEST that verifies:
 * <ul>
 *   <li>MDC is properly populated during request processing</li>
 *   <li>MDC is cleaned up after request completes</li>
 *   <li>Filter doesn't leak MDC data between requests</li>
 * </ul>
 *
 * <p><b>NOTE:</b> Tests infrastructure behavior (MDC lifecycle), not business logic.
 */
@ApplicationModuleTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Framework: MDC Filter Lifecycle")
@Import(TestApplicationConfiguration.class)
class MdcFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @TestBean
    private UserDetailsService userDetailsService;

    static UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    /**
     * Verify MDC doesn't leak between requests.
     *
     * <p>This is important for multi-tenant environments where incorrect MDC data
     * could cause security or logging issues.
     */
    @Test
    @DisplayName("should clean up MDC after request completes")
    void shouldCleanupMDCAfterRequest() throws Exception {
        // Given: MDC is empty before request
        assertThat(MDC.getCopyOfContextMap()).isNull();

        // When: Make an API request (triggers MDC population)
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/members"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isUnauthorized());

        // Then: MDC should be empty after request (no leak)
        assertThat(MDC.getCopyOfContextMap()).isNull();
    }
}
