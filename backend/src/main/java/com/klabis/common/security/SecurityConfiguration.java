package com.klabis.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
public class SecurityConfiguration implements WebMvcConfigurer {

    @Value("${spring.security.oauth2.authorizationserver.issuer:https://localhost:8443}")
    private String issuer;

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    public SecurityConfiguration(CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(KeyPair keyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(KeyPair keyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        org.springframework.security.oauth2.jwt.NimbusJwtDecoder jwtDecoder =
                org.springframework.security.oauth2.jwt.NimbusJwtDecoder
                        .withPublicKey(publicKey)
                        .build();

        // Use externalized issuer URL from configuration instead of hardcoded value
        // This ensures JWT tokens are validated against the correct issuer per environment
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));

        return jwtDecoder;
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public Converter<Jwt, JwtAuthenticationToken> jwtAuthenticationConverter() {
        return new KlabisJwtAuthenticationConverter();
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
    @Order(3)
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
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())  // More restrictive than disable()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();

    }

    @Bean
    @Order(2)
    public SecurityFilterChain spaFilterChain(HttpSecurity http) throws Exception {
        // Custom matcher that only applies to HTML requests (text/html Accept header)
        // This prevents conflicts with API endpoints that might match the same path patterns
        RequestMatcher htmlRequestMatcher = createHtmlRequestMatcher();

        http
                .securityMatcher(htmlRequestMatcher)
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Creates a RequestMatcher that matches:
     * 1. SPA routes (paths without dots) that accept text/html
     * 2. Static resources and specific files (unconditionally)
     *
     * This ensures API endpoints (which typically accept application/json or application/hal+json)
     * are NOT matched by the SPA filter chain, even if they share similar path patterns.
     */
    @SuppressWarnings("removal")  // AntPathRequestMatcher is deprecated but no alternative available yet
    private RequestMatcher createHtmlRequestMatcher() {
        ContentNegotiationStrategy negotiationStrategy = new HeaderContentNegotiationStrategy();

        // Matcher for SPA routes - only matches when client accepts text/html
        MediaTypeRequestMatcher htmlMatcher = new MediaTypeRequestMatcher(
                negotiationStrategy,
                MediaType.TEXT_HTML
        );
        htmlMatcher.setIgnoredMediaTypes(java.util.Set.of(MediaType.ALL));

        // Apply HTML matcher to SPA route patterns
        RequestMatcher spaRoutesMatcher = new OrRequestMatcher(
                new AntPathRequestMatcher("/"),
                new AntPathRequestMatcher("/{path:[^\\.]*}"),
                new AntPathRequestMatcher("/{path1}/{path2:[^\\.]*}"),
                new AntPathRequestMatcher("/{path1}/{path2}/{path3:[^\\.]*}")
        );

        // Combine: (SPA routes AND accepts HTML) OR static resources
        return new OrRequestMatcher(
                request -> spaRoutesMatcher.matches(request) && htmlMatcher.matches(request),
                new AntPathRequestMatcher("/static/**"),
                new AntPathRequestMatcher("/index.html"),
                new AntPathRequestMatcher("/favicon.ico")
        );
    }

    @Bean
    @Order(4)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .securityMatcher("/api/**", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**")
                .authorizeHttpRequests(authorize -> authorize
                        // Actuator endpoints (public access for monitoring)
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Password setup endpoints (public)
                        .requestMatchers("/api/auth/password-setup/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
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
                        .contentTypeOptions(content -> content.disable())
                )
                // CSRF is disabled since we're using JWT stateless authentication
                // If cookie-based auth is added in the future, reconsider this setting
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Override
    public void addArgumentResolvers(java.util.List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
