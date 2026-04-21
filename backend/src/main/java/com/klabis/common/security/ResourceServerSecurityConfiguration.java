package com.klabis.common.security;

import com.klabis.common.users.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

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
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> problem = new HashMap<>();
            problem.put("type", "https://api.klabis.example.com/errors/unauthorized");
            problem.put("title", "Unauthorized");
            problem.put("status", 401);
            problem.put("detail",
                    authException.getMessage() != null ? authException.getMessage() : "Authentication required");

            response.getWriter().write(objectMapper.writeValueAsString(problem));
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> problem = new HashMap<>();
            problem.put("type", "https://api.klabis.example.com/errors/forbidden");
            problem.put("title", "Forbidden");
            problem.put("status", 403);
            problem.put("detail",
                    accessDeniedException.getMessage() != null ? accessDeniedException.getMessage() : "Insufficient authority");

            response.getWriter().write(objectMapper.writeValueAsString(problem));
        };
    }
}
