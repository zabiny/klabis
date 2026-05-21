package com.klabis.calendar.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepts {@code /ical/**} requests that carry a {@code ?token=} query parameter and
 * authenticates them via the iCal PAT mechanism.
 * <p>
 * Placed <em>before</em> {@code BearerTokenAuthenticationFilter} in the resource-server chain.
 * When the {@code token} parameter is absent the filter does nothing — the request falls
 * through to OAuth2 bearer processing.
 */
class IcalTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_PARAM = "token";

    private final RequestMatcher icalMatcher = PathPatternRequestMatcher.withDefaults().matcher("/ical/**");
    private final AuthenticationManager authenticationManager;

    IcalTokenAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !icalMatcher.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String rawToken = request.getParameter(TOKEN_PARAM);
        if (rawToken == null || rawToken.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Authentication authenticated = authenticationManager.authenticate(
                    new IcalTokenAuthenticationToken(rawToken));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authenticated);
            SecurityContextHolder.setContext(context);
            try {
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }
}
