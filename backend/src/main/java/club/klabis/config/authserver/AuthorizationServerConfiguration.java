package club.klabis.config.authserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class AuthorizationServerConfiguration {

    public static final int AUTH_SERVER_SECURITY_ORDER = Ordered.HIGHEST_PRECEDENCE + 5;
    public static final int AUTH_SERVER_LOGIN_PAGE = AUTH_SERVER_SECURITY_ORDER + 10;
    public static final int BEFORE_LOGIN_PAGE = AUTH_SERVER_LOGIN_PAGE - 2;
    public static final int AFTER_LOGIN_PAGE = AUTH_SERVER_LOGIN_PAGE + 2;
    public static final int BEFORE_AUTH_SERVER_SECURITY_ORDER = AUTH_SERVER_SECURITY_ORDER - 2;
    public static final int AFTER_AUTH_SERVER_SECURITY_ORDER = AUTH_SERVER_SECURITY_ORDER + 2;

    private static final String LOVABLE_APP_CLIENT_ID = "aife";

    @Bean
    @Order(AUTH_SERVER_SECURITY_ORDER)
    public SecurityFilterChain authorizationSecurityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider daoAuthenticationProvider
    ) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                // Lovable for sandbox environment uses random URL prefixes -> allowing redirect_uri definition for Lovable web with pattern matching to allow all sandboxes to authenticated against Klabis OAuth2
                .authorizationEndpoint(new WildcardRedirectUriForOAuth2AuthorizationEndpointCustomizer(List.of(
                        LOVABLE_APP_CLIENT_ID)))
                // Is this actually used?? It doesn't seem it is...
                .tokenEndpoint(tokenEndpoint ->
                        tokenEndpoint
                                .authenticationProvider(daoAuthenticationProvider)
                )
                .oidc(Customizer.withDefaults()); // Enable OpenID Connect 1.0

        //request cache for requests between Login Page and Authorization server (it's needed if there would be some application UI with own spring security chain to login user)
        //http.requestCache(applyAuthorizationServerRequestCache());

        http.cors(cors -> cors
                .configurationSource(corsConfigurationSource()));


        // OAuth2 resource server to authenticate OIDC userInfo and/or client registration endpoints
        http.oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()));

        http.exceptionHandling(
                exceptions ->
                        exceptions.authenticationEntryPoint(
                                new LoginUrlAuthenticationEntryPoint(LoginPageSecurityConfiguration.CUSTOM_LOGIN_PAGE)
                        )
        );

        return http.build();
    }

    @Bean
    public OAuth2AuthorizationService authorizationService() {
        // TODO: replace with DB
        return new InMemoryOAuth2AuthorizationService();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            PasswordEncoder passwordEncoder, UserDetailsService userDetailsService
    ) {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        return daoAuthenticationProvider;
    }

    //@Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Povolené origin adresy
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:8080",
                "http://localhost:3000",
                "https://preview--orienter-club-hub.lovable.app",
                "https://*.lovableproject.com",
                "https://klabis.otakar.io",
                "https://wiki.zabiny.club",
                "https://klabis-api-docs.otakar.io"
        ));

        // Povolené OIDC/OAuth2 endpointy
        config.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS"));

        // Povolené hlavičky
        config.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With"
        ));

        // Povolení credentials (důležité pro OAuth2/OIDC flows)
        config.setAllowCredentials(true);

        // Jak dlouho může prohlížeč cachovat CORS response
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplikovat CORS config na všechny OIDC/OAuth2 endpointy
        source.registerCorsConfiguration("/.well-known/openid-configuration", config);
        source.registerCorsConfiguration("/oauth2/**", config);
        source.registerCorsConfiguration("/oauth/**", config);
        source.registerCorsConfiguration("/oidc/**", config);

        return source;
    }

    // if authorization server is running on same instance as UI with own SpringSecurity login mechanism, then it is good to separate authorization server requests flow using own requests cache with own session attribute name
    protected static Customizer<RequestCacheConfigurer<HttpSecurity>> applyAuthorizationServerRequestCache() {
        HttpSessionRequestCache sessionRequestCache = new HttpSessionRequestCache();
        sessionRequestCache.setSessionAttrName("klabis-auth-server-cached-request");

        return httpSecurityRequestCacheConfigurer -> httpSecurityRequestCacheConfigurer.requestCache(
                sessionRequestCache);
    }

}