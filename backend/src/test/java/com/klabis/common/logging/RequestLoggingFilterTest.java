package com.klabis.common.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RequestLoggingFilter MDC lifecycle")
class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @AfterEach
    void clearContexts() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("populates request context, correlation id and user id for an authenticated API request, then clears MDC")
    void populatesMdcForApiRequests() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "ROLE_USER"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/123");
        request.addHeader("X-Correlation-ID", "corr-42");
        Map<String, String> mdcDuringRequest = new HashMap<>();
        FilterChain chain = (req, res) -> mdcDuringRequest.putAll(MDC.getCopyOfContextMap());

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(mdcDuringRequest)
                .containsEntry("requestContext", "GET /members/123")
                .containsEntry("correlationId", "corr-42")
                .containsEntry("userId", "admin");
        assertThat(MDC.getCopyOfContextMap()).isNull();
    }

    @Test
    @DisplayName("does not populate MDC for non-API requests")
    void ignoresNonApiRequests() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "ROLE_USER"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        AtomicBoolean mdcWasEmpty = new AtomicBoolean();
        FilterChain chain = (req, res) -> mdcWasEmpty.set(MDC.getCopyOfContextMap() == null);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(mdcWasEmpty).isTrue();
        assertThat(MDC.getCopyOfContextMap()).isNull();
    }

    @Test
    @DisplayName("skips anonymous authentication")
    void skipsAnonymousAuthentication() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "anonymous-key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api");
        Map<String, String> mdcDuringRequest = new HashMap<>();
        FilterChain chain = (req, res) -> mdcDuringRequest.putAll(MDC.getCopyOfContextMap());

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(mdcDuringRequest).doesNotContainKey("userId");
        assertThat(MDC.getCopyOfContextMap()).isNull();
    }

    @Test
    @DisplayName("clears MDC when request processing fails")
    void clearsMdcWhenChainThrows() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "ROLE_USER"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members");
        FilterChain chain = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), chain))
                .isInstanceOf(IllegalStateException.class);
        assertThat(MDC.getCopyOfContextMap()).isNull();
    }
}
