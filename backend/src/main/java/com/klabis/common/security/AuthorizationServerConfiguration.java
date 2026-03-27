package com.klabis.common.security;

import com.klabis.common.users.Authority;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OAuth2 Authorization Server configuration.
 * <p>
 * Configures Spring Authorization Server for token issuance with JWT access tokens
 * and opaque refresh tokens.
 * <p>
 * Issuer URL is externalized for environment-specific configuration.
 * <p>
 * Registered clients are managed by JdbcRegisteredClientRepository and initialized
 * by BootstrapDataLoader from environment variables for better security.
 */
@Configuration
public class AuthorizationServerConfiguration {

    @Value("${spring.security.oauth2.authorizationserver.issuer:https://localhost:8443}")
    private String issuer;

    private final AuthorizationServerCustomizer authorizationServerCustomizer;

    public AuthorizationServerConfiguration(ObjectProvider<AuthorizationServerCustomizer> authorizationServerCustomizerProvider) {
        this.authorizationServerCustomizer = authorizationServerCustomizerProvider.getIfAvailable(() -> AuthorizationServerCustomizer.EMPTY_CUSTOMIZER);
    }

    /**
     * JDBC-based registered client repository.
     * Clients are loaded from database and initialized by BootstrapDataLoader.
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcOperations jdbcOperations) {
        return new JdbcRegisteredClientRepository(jdbcOperations);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcOperations jdbcOperations,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcOperations jdbcOperations,
            RegisteredClientRepository registeredClientRepository) {

        return new JdbcOAuth2AuthorizationConsentService(jdbcOperations, registeredClientRepository);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                // For client_credentials grant, use authorized scopes as authorities
                if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
                    var authorizedScopes = context.getAuthorizedScopes();
                    context.getClaims().claim(KlabisOAuth2ClaimNames.CLAIM_AUTHORITIES, new HashSet<>(authorizedScopes));
                } else {
                    // For user-based grants (authorization_code, etc.), use user authorities
                    // Only include known Klabis authorities, filtering out framework-added
                    // authorities like Spring Security 7 MFA factors (FACTOR_PASSWORD)
                    context.getClaims().claim(KlabisOAuth2ClaimNames.CLAIM_AUTHORITIES,
                            context.getPrincipal().getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .filter(Authority::isKnownAuthority)
                                    .collect(Collectors.toCollection(ArrayList::new)));
                }

                authorizationServerCustomizer.customizeAccessTokenClaims(context.getPrincipal().getName(),
                        context.getClaims(),
                        context.getAuthorizationGrantType());
            } else if (context.getTokenType().getValue().equals("id_token")) {
                // ID Token claims for OpenID Connect
                String subject = context.getPrincipal().getName();

                // Always add user_name claim for ID tokens (except client_credentials grant)
                if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
                    context.getClaims().claim(KlabisOAuth2ClaimNames.CLAIM_USER_NAME, subject);
                }

                // Add profile claims (user_name, given_name, family_name) for OIDC profile scope
                authorizationServerCustomizer.customizeIdTokenClaims(subject,
                        context.getClaims(),
                        context.getAuthorizationGrantType());
            }
        };
    }

    /**
     * Custom UserInfo endpoint response for OpenID Connect.
     * <p>
     * Spring Authorization Server provides the UserInfo endpoint (/oauth2/userinfo) automatically
     * when OIDC is enabled. This customizer implements OIDC-compliant scope-based access control
     * for user claims.
     * <p>
     * Scope-to-Claims Mapping:
     * - openid: sub (always returned)
     * - profile: given_name, family_name, registrationNumber, updated_at
     * - email: email, email_verified (only if Member has email)
     * <p>
     * For admin users who don't have a linked Member entity, only sub is returned.
     */
    private Function<OidcUserInfoAuthenticationContext, OidcUserInfo> oidcUserInfoMapper() {
        return context -> {
            // Task 2.1: Extract authorized scopes from context
            Set<String> scopes = context.getAuthorization().getAuthorizedScopes();

            OidcUserInfoAuthenticationToken authentication = context.getAuthentication();
            JwtAuthenticationToken principal = (JwtAuthenticationToken) authentication.getPrincipal();

            // Extract subject from token (always present)
            String subject = principal.getToken().getSubject();
            if (subject == null) {
                return OidcUserInfo.builder().subject("unknown").build();
            }

            // Task 2.2: Start with only sub claim (openid scope)
            OidcUserInfo.Builder builder = OidcUserInfo.builder().subject(subject);

            // Add profile-scoped claims (is_member, user_name, and member profile data)
            // For users without profile scope, only sub claim is returned
            if (scopes.contains("profile")) {
                // Always add user_name claim (username from authentication)
                builder.claim("user_name", subject);

                authorizationServerCustomizer.customizeOidcUserInfo(subject, scopes, builder);
            }

            return builder.build();
        };
    }


    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource) throws Exception {

        http
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                )
                .oauth2AuthorizationServer(authorizationServer -> {
                    http.securityMatcher(authorizationServer.getEndpointsMatcher());
                    authorizationServer
                            .oidc(oidc -> oidc
                                    .userInfoEndpoint(userInfo -> userInfo
                                            .userInfoMapper(oidcUserInfoMapper())
                                    )
                            );
                })
                // Enable CORS for OAuth2 endpoints (including /oauth2/token)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
//                // Accept access tokens for User Info and/or Client Registration
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                // Redirect to login page when not authenticated for authorization endpoint
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML))
                );
;
        return http.build();
    }

    /**
     * Security filter chain for the login page.
     * Separate from authorization server chain because securityMatcher limits
     * the auth server chain to OAuth2 protocol endpoints only.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain authorizationServerLoginSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/login")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .formLogin(form -> form.loginPage("/login").permitAll())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/login"));

        return http.build();
    }

}
