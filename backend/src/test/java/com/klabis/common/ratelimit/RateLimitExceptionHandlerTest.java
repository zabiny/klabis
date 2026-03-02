package com.klabis.common.ratelimit;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.users.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test for {@link RateLimitExceptionHandler}.
 * <p>
 * Verifies that HTTP exceptions are properly mapped to RFC 7807 Problem Details
 * with correct HTTP status codes.
 */
@WebMvcTest(controllers = ErrorHandlingTestController.class)
@ActiveProfiles("test")
@Import(EncryptionConfiguration.class)
@MockitoBean(types = {UserDetailsService.class, UserService.class})
class RateLimitExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ErrorHandlingTestController controller;

    @Test
    @WithKlabisMockUser
    void shouldReturnMethodNotAllowedWhenCallingPostEndpointWithGet() throws Exception {
        controller.setExceptionToThrow(new RateLimitExceededException("Example exception"));

        // Given: POST /api/events/{id}/publish endpoint exists
        // When: Client sends GET request instead of POST
        mockMvc.perform(get("/throwingApi")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Returns HTTP 405 Method Not Allowed
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Too Many Requests"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.detail").value("Too many requests. Please try again later"));
    }
}

@RestController
class ErrorHandlingTestController {

    private Exception exceptionToThrow = new RuntimeException("Example exception");

    public void setExceptionToThrow(Exception ex) {
        this.exceptionToThrow = ex;
    }

    @GetMapping("/throwingApi")
    public void throwingApi() throws Exception {
        throw exceptionToThrow;
    }

}