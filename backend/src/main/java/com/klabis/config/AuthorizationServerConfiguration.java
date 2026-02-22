package com.klabis.config;

import com.klabis.members.MemberDto;
import com.klabis.members.Members;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
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
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer(Members members) {
        return context -> {
            if (context.getTokenType().getValue().equals("access_token")) {
                // For client_credentials grant, use authorized scopes as authorities
                if (context.getAuthorizationGrantType().getValue().equals("client_credentials")) {
                    var authorizedScopes = context.getAuthorizedScopes();
                    context.getClaims().claim("authorities", new HashSet<>(authorizedScopes));
                } else {
                    // For user-based grants (authorization_code, etc.), use user authorities
                    context.getClaims().claim("user_name",
                            context.getPrincipal().getName());
                    context.getClaims().claim("authorities",
                            context.getPrincipal().getAuthorities().stream()
                                    .map(auth -> auth.getAuthority())
                                    .collect(Collectors.toCollection(ArrayList::new)));
                }
            } else if (context.getTokenType().getValue().equals("id_token")) {
                // ID Token claims for OpenID Connect
                // Note: Standard claims (sub, iss, aud, exp, iat, auth_time) are handled
                // automatically by Spring Authorization Server. We only add custom claims here.
                String subject = context.getPrincipal().getName();
                context.getClaims().claim("user_name", subject);

                // Add profile claims (given_name, family_name) for OIDC profile scope
                members.findByRegistrationNumber(subject)
                        .ifPresent(member -> {
                            context.getClaims().claim("given_name", member.firstName());
                            context.getClaims().claim("family_name", member.lastName());
                            context.getClaims().claim("preferred_username", subject);
                        });
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
    private Function<OidcUserInfoAuthenticationContext, OidcUserInfo> oidcUserInfoMapper(
            Members members) {
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

                members.findByRegistrationNumber(subject).ifPresentOrElse(memberDto -> {
                    // User has Member profile - add is_member=true and member claims
                    builder.claim("is_member", true);
                    addProfileClaims(builder, scopes, memberDto);
                    addEmailClaims(builder, scopes, memberDto);
                }, () -> {
                    // user without member
                    builder.claim("is_member", false);
                });

            }

            return builder.build();
        };
    }

    /**
     * Adds OIDC profile scope claims to UserInfo response for members.
     * <p>
     * Maps Member data to OIDC claims:
     * - given_name (first name)
     * - family_name (last name)
     * - updated_at (profile last modification timestamp)
     * <p>
     * Note: user_name and is_member claims are added by oidcUserInfoMapper before calling this method.
     */
    private void addProfileClaims(OidcUserInfo.Builder builder, Set<String> scopes, MemberDto member) {
        if (scopes.contains("profile")) {
            builder.givenName(member.firstName())
                   .familyName(member.lastName())
                   .claim("updated_at", member.lastModifiedAt());
        }
    }

    /**
     * Adds OIDC email scope claims to UserInfo response.
     * <p>
     * Task 2.5-2.6: Maps Member email to standard OIDC claims:
     * - email (member's email address)
     * - email_verified (always false until email verification is implemented)
     * <p>
     * Omits email claims if Member has no email (null-safe).
     */
    private void addEmailClaims(OidcUserInfo.Builder builder, Set<String> scopes, MemberDto member) {
        if (scopes.contains("email") && member.email() != null) {
            builder.email(member.email())
                   .emailVerified(false);
        }
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            Members members,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(oidc -> oidc
                        .userInfoEndpoint(userInfo -> userInfo
                                .userInfoMapper(oidcUserInfoMapper(members))
                        )
                );

        http
                // Enable CORS for OAuth2 endpoints (including /oauth2/token)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Accept access tokens for User Info and/or Client Registration
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                // Redirect to login page when not authenticated for authorization endpoint
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain authorizationServerLoginSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/login")
                .formLogin(Customizer.withDefaults());

        return http.build();
    }

}
