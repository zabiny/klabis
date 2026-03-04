package com.klabis.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that adds HTTP request context to MDC for all API requests.
 *
 * <p>This filter populates the Mapped Diagnostic Context (MDC) with:
 * <ul>
 *   <li>requestContext: HTTP method and path (e.g., "GET /members/123")</li>
 *   <li>userId: Authenticated user ID (if present)</li>
 *   <li>correlationId: Request correlation ID for distributed tracing</li>
 * </ul>
 *
 * <p>Only processes requests to {@code /api/**} endpoints. Non-API requests bypass
 * this filter without MDC modification.
 *
 * <p>Supports distributed tracing via {@code X-Correlation-ID} or {@code X-Request-ID}
 * headers. If not provided, a new UUID is generated.
 *
 * <p>The {@code /api} prefix is stripped from the path for cleaner logs.
 * For example, {@code /api/members/123} becomes {@code GET /members/123}.
 */
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_CONTEXT = "requestContext";
    private static final String USER_ID = "userId";
    private static final String CORRELATION_ID = "correlationId";
    private static final String API_PREFIX = "/api";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only process /api/** endpoints
        if (!path.startsWith(API_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract HTTP method and path (strip /api prefix)
            String method = request.getMethod();
            String cleanPath = path.substring(API_PREFIX.length());

            // Build request context: "METHOD /path"
            String requestContext = method + " " + cleanPath;
            MDC.put(REQUEST_CONTEXT, requestContext);

            // Get or generate correlation ID
            String correlationId = getOrGenerateCorrelationId(request);
            MDC.put(CORRELATION_ID, correlationId);

            filterChain.doFilter(request, response);

        } finally {
            // CRITICAL: Clean up MDC to prevent memory leaks
            MDC.remove(REQUEST_CONTEXT);
            MDC.remove(USER_ID);
            MDC.remove(CORRELATION_ID);
            MDC.clear();
        }
    }

    /**
     * Gets or generates a correlation ID for the request.
     *
     * <p>Checks for {@code X-Correlation-ID} and {@code X-Request-ID} headers.
     * If neither is present, generates a new UUID.
     *
     * @param request the HTTP request
     * @return the correlation ID
     */
    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        // Check for existing correlation ID in header
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = request.getHeader("X-Request-ID");
        }
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    /**
     * Extracts the user ID from the Spring Security context.
     *
     * @return the authenticated user's ID, or {@code null} if not authenticated
     */
    private String extractUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.warn("Failed to extract user ID from security context", e);
        }
        return null;
    }

    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher
            (ApplicationEventPublisher applicationEventPublisher) {
        return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent ignored) {
        // Extract user ID from security context
        String userId = extractUserId();
        if (userId != null) {
            MDC.put(USER_ID, userId);
        }
    }
}
