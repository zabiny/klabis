package com.klabis.config;

import com.klabis.members.Members;
import com.klabis.members.RegistrationNumber;
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

import java.util.ArrayList;
import java.util.HashSet;
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
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            if (context.getTokenType().getValue().equals("access_token")) {
                // For client_credentials grant, use authorized scopes as authorities
                if (context.getAuthorizationGrantType().getValue().equals("client_credentials")) {
                    var authorizedScopes = context.getAuthorizedScopes();
                    context.getClaims().claim("authorities", new HashSet<>(authorizedScopes));
                } else {
                    // For user-based grants (authorization_code, etc.), use user authorities
                    context.getClaims().claim("registrationNumber",
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
                context.getClaims().claim("registrationNumber", context.getPrincipal().getName());
            }
        };
    }

    /**
     * Custom UserInfo endpoint response for OpenID Connect.
     * <p>
     * Spring Authorization Server provides the UserInfo endpoint (/oauth2/userinfo) automatically
     * when OIDC is enabled. This customizer loads member profile data from the Member entity
     * and adds firstName and lastName to the UserInfo response.
     * <p>
     * UserInfo claims:
     * - sub: registrationNumber (standard OIDC claim)
     * - registrationNumber: member's registration number
     * - firstName: member's first name (from Member entity)
     * - lastName: member's last name (from Member entity)
     * <p>
     * For admin users who don't have a linked Member entity, only sub and registrationNumber are returned.
     */
    private Function<OidcUserInfoAuthenticationContext, OidcUserInfo> oidcUserInfoMapper(
            Members members) {
        return context -> {
            OidcUserInfoAuthenticationToken authentication = context.getAuthentication();
            JwtAuthenticationToken principal = (JwtAuthenticationToken) authentication.getPrincipal();

            // Extract registrationNumber from token subject
            String registrationNumber = principal.getToken().getSubject();

            // Build UserInfo with standard claims
            OidcUserInfo.Builder userInfoBuilder = OidcUserInfo.builder()
                    .subject(registrationNumber)
                    .claim("registrationNumber", registrationNumber);

            // Load Member entity by registrationNumber and add profile claims
            // Only query if registrationNumber has valid format (admin users have invalid format)
            if (RegistrationNumber.isRegistrationNumber(registrationNumber)) {
                members.findByRegistrationNumber(RegistrationNumber.of(registrationNumber))
                        .ifPresent(member -> {
                            userInfoBuilder
                                    .claim("firstName", member.getFirstName())
                                    .claim("lastName", member.getLastName());
                        });
            }

            return userInfoBuilder.build();
        };
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            Members members) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(oidc -> oidc
                        .userInfoEndpoint(userInfo -> userInfo
                                .userInfoMapper(oidcUserInfoMapper(members))
                        )
                );

        http
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
