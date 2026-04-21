package com.klabis.common.security;

import com.klabis.common.users.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

/**
 * JWT authentication converter, account status filter, and custom error handlers for the resource server.
 * <p>
 * The actual API filter chain ({@code defaultSecurityFilterChain}) lives in
 * {@link WebSecurityCommonConfiguration} so that {@code @WebMvcTest} slices auto-discover it
 * via the {@code @EnableWebSecurity}-annotated class. This class provides only the supporting
 * beans that the filter chain depends on.
 * <p>
 * CSRF Protection:
 * - CSRF is DISABLED for the API filter chain (see WebSecurityCommonConfiguration.defaultSecurityFilterChain)
 * - Rationale: JWT tokens are stored in memory/Authorization header, not cookies
 * - Stateless authentication is immune to CSRF attacks
 * - If cookie-based authentication is added in the future, CSRF protection MUST be enabled
 */
@Configuration
@Import(JwtKeysConfiguration.class)
public class ResourceServerSecurityConfiguration {

    @Bean
    public Converter<Jwt, JwtAuthenticationToken> jwtAuthenticationConverter() {
        return new KlabisJwtAuthenticationConverter();
    }

    @Bean
    public AccountStatusValidationFilter accountStatusValidationFilter(UserService userService, ObjectMapper objectMapper) {
        return new AccountStatusValidationFilter(userService, objectMapper);
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, authException) -> writeProblemDetail(
                response, objectMapper,
                HttpStatus.UNAUTHORIZED,
                "https://api.klabis.example.com/errors/unauthorized",
                "Unauthorized",
                authException.getMessage() != null ? authException.getMessage() : "Authentication required");
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, accessDeniedException) -> writeProblemDetail(
                response, objectMapper,
                HttpStatus.FORBIDDEN,
                "https://api.klabis.example.com/errors/forbidden",
                "Forbidden",
                accessDeniedException.getMessage() != null ? accessDeniedException.getMessage() : "Insufficient authority");
    }

    private static void writeProblemDetail(HttpServletResponse response, ObjectMapper objectMapper,
                                           HttpStatus status, String type, String title, String detail) throws java.io.IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(type));
        problem.setTitle(title);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
