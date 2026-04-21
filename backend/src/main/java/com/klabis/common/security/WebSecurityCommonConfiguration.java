package com.klabis.common.security;

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

/**
 * Common web security configuration: method security, API resource server, SPA, docs, and H2 console filter chains.
 * <p>
 * Owns {@link EnableWebSecurity} and {@link EnableMethodSecurity} — exactly one {@code @Configuration}
 * class in the context carries these, and it is this one.
 * <p>
 * Implements {@link WebMvcConfigurer} so that {@code @WebMvcTest} slices auto-discover this class:
 * Spring Boot's {@code WebMvcTypeExcludeFilter} includes {@code WebMvcConfigurer} implementors,
 * which causes all {@code @Bean} methods on this class to be loaded in test slices. Without this
 * marker interface, the filter chain beans (including {@code defaultSecurityFilterChain}) would
 * be absent from the slice context and Spring Security would fall back to its auto-configured chain.
 * <p>
 * Authentication Architecture:
 * - JWT-based stateless authentication (no server-side sessions) for API chain
 * - OAuth2 Authorization Server for token issuance (see AuthorizationServerConfiguration)
 * <p>
 * Configuration:
 * - Externalizes issuer URL for environment-specific configuration
 * - Security headers: CSP, Frame Options, HSTS, X-Content-Type-Options
 * - H2 console access restricted to dev profile only
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import(ResourceServerSecurityConfiguration.class)
public class WebSecurityCommonConfiguration implements WebMvcConfigurer {

    @Bean
    public HasAuthorityMethodInterceptor hasAuthorityMethodInterceptor() {
        return new HasAuthorityMethodInterceptor();
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
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/auth/password-setup/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterAfter(accountStatusValidationFilter, BearerTokenAuthenticationFilter.class)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none';")
                        )
                        .frameOptions(frame -> frame.deny())
                        .xssProtection(xss -> xss.disable())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable)
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(PATHS));

        return http.build();
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
}
