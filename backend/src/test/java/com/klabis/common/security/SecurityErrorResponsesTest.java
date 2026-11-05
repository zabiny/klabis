package com.klabis.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Resource server error responses")
class SecurityErrorResponsesTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();
    private final ResourceServerSecurityConfiguration configuration = new ResourceServerSecurityConfiguration();

    @Test
    @DisplayName("entry point answers 401 with relative type /errors/unauthorized")
    void entryPoint() throws Exception {
        var response = new MockHttpServletResponse();

        configuration.customAuthenticationEntryPoint(objectMapper)
                .commence(new MockHttpServletRequest(), response, new BadCredentialsException("bad"));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(body.get("type").asString()).isEqualTo("/errors/unauthorized");
    }

    @Test
    @DisplayName("access denied handler answers 403 with relative type /errors/forbidden")
    void accessDenied() throws Exception {
        var response = new MockHttpServletResponse();

        configuration.customAccessDeniedHandler(objectMapper)
                .handle(new MockHttpServletRequest(), response, new AccessDeniedException("no"));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("type").asString()).isEqualTo("/errors/forbidden");
    }
}
