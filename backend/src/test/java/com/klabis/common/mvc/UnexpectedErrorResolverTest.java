package com.klabis.common.mvc;

import com.klabis.common.CommonInfrastructureWebMvcSetup;
import com.klabis.common.WithKlabisMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UnexpectedErrorResolverTest.TestController.class)
@CommonInfrastructureWebMvcSetup
@DisplayName("Unhandled exceptions")
class UnexpectedErrorResolverTest {

    @Autowired
    MockMvc mockMvc;

    @MvcComponent
    @RestController
    static class TestController {
        @GetMapping(value = "/test/unexpected", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        String unexpected() {
            throw new NullPointerException("sensitive internal detail");
        }

        @GetMapping(value = "/test/illegal-argument", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        String illegalArgument() {
            throw new IllegalArgumentException("sensitive internal detail");
        }
    }

    @Test
    @DisplayName("should answer 500 unexpected with generic detail and without the raw message")
    @WithKlabisMockUser
    void shouldAnswerUnexpected() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/errors/unexpected"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Unexpected error"))
                .andExpect(jsonPath("$.properties").doesNotExist());
    }

    @Test
    @DisplayName("should answer 400 invalid-request for IllegalArgumentException without the raw message")
    @WithKlabisMockUser
    void shouldAnswerInvalidRequest() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/errors/invalid-request"))
                .andExpect(jsonPath("$.detail").value("Invalid request"));
    }

    @Test
    @DisplayName("should answer 404 not-found for unknown path")
    @WithKlabisMockUser
    void shouldAnswerNotFoundForUnknownPath() throws Exception {
        mockMvc.perform(get("/test/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("/errors/not-found"));
    }

    @Test
    @DisplayName("resolver leaves security exceptions to the security layer")
    void resolverIgnoresSecurityExceptions() {
        var resolver = new UnexpectedErrorResolver(tools.jackson.databind.json.JsonMapper.builder().build());
        var request = new org.springframework.mock.web.MockHttpServletRequest();

        org.assertj.core.api.Assertions.assertThat(resolver.resolveException(request,
                new org.springframework.mock.web.MockHttpServletResponse(), null,
                new org.springframework.security.access.AccessDeniedException("denied"))).isNull();
        org.assertj.core.api.Assertions.assertThat(resolver.resolveException(request,
                new org.springframework.mock.web.MockHttpServletResponse(), null,
                new org.springframework.security.authentication.BadCredentialsException("bad"))).isNull();
    }
}
