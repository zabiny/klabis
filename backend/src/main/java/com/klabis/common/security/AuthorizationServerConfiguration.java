package com.klabis.common.security;

import com.klabis.common.users.Authority;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
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

    private static final Map<String, Set<Authority>> SCOPE_TO_AUTHORITIES = Map.of(
            Authority.MEMBERS_SCOPE, EnumSet.of(Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE, Authority.MEMBERS_PERMISSIONS),
            Authority.EVENTS_SCOPE, EnumSet.of(Authority.EVENTS_READ, Authority.EVENTS_MANAGE),
            Authority.CALENDAR_SCOPE, EnumSet.of(Authority.CALENDAR_MANAGE)
    );

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                // For client_credentials grant, expand scopes to fine-grained authorities
                // so that API endpoints using @PreAuthorize("hasAuthority('MEMBERS:READ')") work correctly
                if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
                    var authorizedScopes = context.getAuthorizedScopes();
                    var authorities = authorizedScopes.stream()
                            .flatMap(scope -> SCOPE_TO_AUTHORITIES.getOrDefault(scope, Set.of()).stream())
                            .map(Authority::getValue)
                            .collect(Collectors.toCollection(HashSet::new));
                    context.getClaims().claim(KlabisOAuth2ClaimNames.CLAIM_AUTHORITIES, authorities);
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

                // Remove sid claim to avoid logout validation bug in Spring Security 7 (GH-16824):
                // JwtGenerator sets sid=raw sessionId but OidcLogoutAuthenticationProvider
                // validates against SHA256(sessionId), causing 400 invalid_token on logout.
                context.getClaims().claims(claims -> claims.remove("sid"));

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
            CorsConfigurationSource corsConfigurationSource,
            RegisteredClientRepository registeredClientRepository) throws Exception {

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
                // Per OIDC spec (section 3.1.2.1), prompt=none MUST return login_required error
                // to redirect_uri — it MUST NOT redirect to a login page. The prompt=none entry
                // point is registered first so it takes priority over the login redirect for those
                // requests. Regular unauthenticated HTML requests still redirect to /login.
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                promptNoneAuthenticationEntryPoint(registeredClientRepository),
                                promptNoneRequestMatcher())
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML))
                );
        return http.build();
    }

    /**
     * Matches /oauth2/authorize requests carrying {@code prompt=none}.
     * Used to register the OIDC-compliant entry point before the login-redirect entry point.
     */
    private static RequestMatcher promptNoneRequestMatcher() {
        return new AndRequestMatcher(
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML),
                request -> "none".equals(request.getParameter("prompt"))
        );
    }

    /**
     * Authentication entry point for {@code prompt=none} requests.
     * <p>
     * Per OIDC spec (section 3.1.2.1), when {@code prompt=none} is set and the user is not
     * authenticated, the authorization server MUST redirect back to the {@code redirect_uri}
     * with {@code error=login_required}. It MUST NOT display any UI (including a login page).
     * <p>
     * This entry point handles the case where the request is intercepted by Spring Security's
     * {@code ExceptionTranslationFilter} before reaching the authorization endpoint filter —
     * which happens because {@code authorizeHttpRequests(...anyRequest().authenticated())}
     * rejects unauthenticated requests early in the filter chain.
     * <p>
     * The {@code redirect_uri} is validated against registered client URIs before redirecting
     * to prevent this entry point from being used as an open redirector. Because
     * {@code ExceptionTranslationFilter} fires before the authorization endpoint's own
     * {@code redirect_uri} validation, we must perform the check ourselves here.
     */
    private static AuthenticationEntryPoint promptNoneAuthenticationEntryPoint(
            RegisteredClientRepository registeredClientRepository) {
        return (request, response, authException) -> {
            String clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID);
            String redirectUri = request.getParameter(OAuth2ParameterNames.REDIRECT_URI);

            if (clientId == null || clientId.isBlank()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "client_id is required for prompt=none");
                return;
            }

            var registeredClient = registeredClientRepository.findByClientId(clientId);
            if (registeredClient == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown client_id");
                return;
            }

            if (redirectUri == null || redirectUri.isBlank()
                    || !registeredClient.getRedirectUris().contains(redirectUri)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "redirect_uri is missing or not registered for this client");
                return;
            }

            String state = request.getParameter(OAuth2ParameterNames.STATE);
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam(OAuth2ParameterNames.ERROR, "login_required")
                    .queryParam(OAuth2ParameterNames.ERROR_DESCRIPTION, "User is not authenticated");
            if (state != null) {
                builder.queryParam(OAuth2ParameterNames.STATE, state);
            }
            response.sendRedirect(builder.build().toUriString());
        };
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
