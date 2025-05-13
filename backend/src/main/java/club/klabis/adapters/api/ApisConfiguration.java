package club.klabis.adapters.api;

import club.klabis.config.authserver.AuthorizationServerConfiguration;
import club.klabis.domain.appusers.ApplicationGrant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyUtils;
import org.springframework.security.authorization.method.PrePostTemplateDefaults;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@EnableWebSecurity
@EnableMethodSecurity
@EnableSpringDataWebSupport  // konverze ID -> domain (napr. integer -> Event). melo by snad fungovat az se dodela inMemory repository jako plny SpringRest repository
@Configuration(proxyBeanMethods = false)
public class ApisConfiguration {

    public static RequestMatcher API_ENDPOINTS_MATCHER = new MediaTypeRequestMatcher(MediaType.APPLICATION_JSON,
            MediaType.valueOf("application/hal+json"),
            MediaType.valueOf("application/klabis+json"));

    public ApisConfiguration(KlabisApplicationUserDetailsService applicationUserService) {
        this.applicationUserService = applicationUserService;
    }

    @Bean
    @Order(AuthorizationServerConfiguration.AFTER_LOGIN_PAGE)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(API_ENDPOINTS_MATCHER)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS)
                        .permitAll()    // for CORS preflight requests - OPTIONS must not be authorized
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer((oauth2) -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(klabisMemberEnhanceAuthentication())))
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .build();
    }

    // to enable spring security annotation templating like in @HasGrant
    @Bean
    static PrePostTemplateDefaults prePostTemplateDefaults() {
        return new PrePostTemplateDefaults();
    }

    // https://stackoverflow.com/questions/69100420/spring-oauth2-resource-server-load-synchronized-user-from-database

    private final KlabisApplicationUserDetailsService applicationUserService;

    private Converter<Jwt, KlabisUserAuthentication> klabisMemberEnhanceAuthentication() {
        return source -> applicationUserService.getApplicationUserForUsername(source.getSubject())
                .map(user -> KlabisUserAuthentication.authenticated(user, source))
                .orElseGet(() -> KlabisUserAuthentication.noUser(source));
    }

    @Bean
    static RoleHierarchy customizedRoleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        String hierarchyDef = RoleHierarchyUtils.roleHierarchyFromMap(
                Map.of("ROLE_ADMIN",
                        List.of(ApplicationGrant.MEMBERS_EDIT.name(), ApplicationGrant.MEMBERS_REGISTER.name()))
        );
        hierarchy.setHierarchy(hierarchyDef);
        return hierarchy;
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Povolené origin adresy
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:8080",
                "https://preview--orienter-club-hub.lovable.app",
                "https://*.lovableproject.com",
                "https://klabis.otakar.io"
//                "https://wiki.zabiny.club",
//                "https://klabis-api-docs.otakar.io"
        ));

        //config.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS"));

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
        // Aplikovat CORS config na všechny endpointy resene touto security configuraci (= API endpointy)
        source.registerCorsConfiguration("/**", config);

        return source;
    }

}
