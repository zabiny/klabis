package com.klabis.common.security;

import com.klabis.common.users.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Security configuration for OAuth2 Authorization Server and Resource Server.
 * <p>
 * Authentication Architecture:
 * - JWT-based stateless authentication (no server-side sessions)
 * - Resource server validates JWT tokens on every request
 * - OAuth2 Authorization Server for token issuance
 * <p>
 * Authorization Architecture:
 * - Role-based access control (RBAC) with custom authorities
 * - Method-level security with @PreAuthorize
 * - Scope-based permissions for OAuth2 clients
 * <p>
 * CSRF Protection:
 * - CSRF is DISABLED for this API (see defaultSecurityFilterChain)
 * - Rationale: JWT tokens are stored in memory/Authorization header, not cookies
 * - Stateless authentication is immune to CSRF attacks
 * - If cookie-based authentication is added in the future, CSRF protection MUST be enabled
 * <p>
 * Configuration:
 * - Externalizes issuer URL for environment-specific configuration
 * - Security headers: CSP, Frame Options, HSTS, X-Content-Type-Options
 * - H2 console access restricted to dev profile only
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import(JwtKeysConfiguration.class)
public class SecurityConfiguration implements WebMvcConfigurer {

    @Bean
    public HasAuthorityMethodInterceptor hasAuthorityMethodInterceptor() {
        return new HasAuthorityMethodInterceptor();
    }

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

    @Bean
    @Order(2)
    @ConditionalOnProperty(value = "spring.h2.console.enabled", havingValue = "true")
    public SecurityFilterChain h2FilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/h2-console/**")
                .authorizeHttpRequests(authorize -> authorize
                        // H2 console is only available in dev profile
                        // In production profile, H2 console is explicitly disabled via application.yml
                        .requestMatchers("/h2-console/**")
                        .permitAll()  // Only accessible when H2 console is enabled (dev profile)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())  // More restrictive than disable()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();

    }

    /**
     * Secures the developer documentation under {@code /docs/**}.
     * <p>
     * Unlike the API chain (Bearer JWT) and the SPA chain (permitAll), the documentation
     * is accessed directly by a browser URL — there is no SPA wrapper that could attach a
     * Bearer token. Authentication is therefore session-based: the HTTP session established
     * by the {@code /login} form (see {@code authorizationServerLoginSecurityFilterChain})
     * is reused here. Unauthenticated requests are redirected to {@code /login}.
     * <p>
     * Must be ordered before the SPA chain, otherwise the SPA HTML matcher would swallow
     * {@code /docs/**} requests (browsers send {@code Accept: text/html}).
     */
    @Bean
    @Order(3)
    public SecurityFilterChain docsFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/docs/**")
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                )
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );
        return http.build();
    }

    @Bean
    @Order(4)
    public SecurityFilterChain spaFilterChain(HttpSecurity http) throws Exception {
        // Custom matcher that only applies to HTML requests (text/html Accept header)
        // This prevents conflicts with API endpoints that might match the same path patterns
        RequestMatcher htmlRequestMatcher = createHtmlRequestMatcher();

        http
                .securityMatcher(htmlRequestMatcher)
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                )
                //.csrf(csrf -> csrf.ignoringRequestMatchers(htmlRequestMatcher))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Allow same-origin framing so OAuth2 silent-renew iframe (silent-renew.html)
                // can load inside the SPA. Default Spring Security X-Frame-Options: DENY
                // would block the iframe and prevent the postMessage → token exchange flow.
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }

    /**
     * Creates a RequestMatcher that matches:
     * 1. SPA routes (paths without dots) that accept text/html
     * 2. Static resources and specific files (unconditionally)
     * <p>
     * This ensures API endpoints (which typically accept application/json or application/hal+json)
     * are NOT matched by the SPA filter chain, even if they share similar path patterns.
     */
    private RequestMatcher createHtmlRequestMatcher() {
        ContentNegotiationStrategy negotiationStrategy = new HeaderContentNegotiationStrategy();

        // Matcher for SPA routes - only matches when client accepts text/html
        MediaTypeRequestMatcher htmlMatcher = new MediaTypeRequestMatcher(
                negotiationStrategy,
                MediaType.TEXT_HTML
        );
        htmlMatcher.setIgnoredMediaTypes(java.util.Set.of(MediaType.ALL));

        var matcher = PathPatternRequestMatcher.withDefaults();

        // Apply HTML matcher to SPA route patterns
        RequestMatcher spaRoutesMatcher = new OrRequestMatcher(
                matcher.matcher("/"),
                matcher.matcher("/{path}"),
                matcher.matcher("/{path1}/{path2}"),
                matcher.matcher("/{path1}/{path2}/{path3}")
        );

        // Combine: (SPA routes AND accepts HTML) OR static resources
        return new OrRequestMatcher(
                request -> spaRoutesMatcher.matches(request) && htmlMatcher.matches(request),
                matcher.matcher("/static/**"),
                matcher.matcher("/index.html"),
                matcher.matcher("/favicon.ico")
        );
    }

    @Bean
    @Order(5)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource,
            Converter<Jwt, JwtAuthenticationToken> jwtAuthenticationConverter,
            AccountStatusValidationFilter accountStatusValidationFilter) throws Exception {

        final String[] PATHS = {"/api/**", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**"};

        http
                .securityMatcher(PATHS)
                .authorizeHttpRequests(authorize -> authorize
                        // Actuator endpoints (public access for monitoring)
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Password setup endpoints (public)
                        .requestMatchers("/api/auth/password-setup/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterAfter(accountStatusValidationFilter, BearerTokenAuthenticationFilter.class)
                // Enable CORS configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Security headers for enhanced protection
                .headers(headers -> headers
                        // Content Security Policy to prevent XSS attacks
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none';")
                        )
                        // Prevent clickjacking attacks
                        .frameOptions(frame -> frame.deny())
                        // Disable X-XSS-Protection (modern browsers don't need it, CSP is sufficient)
                        .xssProtection(xss -> xss.disable())
                        // HSTS for HTTPS enforcement (only enable in production with HTTPS)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        // Prevent browser from MIME-sniffing responses
                        .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable)
                )
                // Configure CSRF to ignore stateless API and documentation endpoints,
                // while keeping CSRF protection enabled for any other browser-facing routes.
                .csrf(csrf -> csrf.ignoringRequestMatchers(PATHS));

        return http.build();
    }

}
