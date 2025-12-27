package club.klabis.shared.config.restapi;

import club.klabis.shared.config.authserver.AuthorizationServerConfiguration;
import club.klabis.shared.config.security.ApplicationGrant;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.AbstractRequestLoggingFilter;

import java.util.Arrays;

@EnableWebSecurity
@EnableMethodSecurity
@EnableSpringDataWebSupport
// conversion of ID -> domain (e.g., integer -> Event). It should work once the in-memory repository is completed as a full SpringRest repository. It must be removed if Spring Data REST is added (it conflicts with it as it creates duplicate pageable mapping)
@Configuration
@Import(ApiExceptionHandlers.class)
public class ApisConfiguration {

    private final KlabisPrincipalSource klabisPrincipalSource;

    public ApisConfiguration(KlabisPrincipalSource klabisPrincipalSource) {
        this.klabisPrincipalSource = klabisPrincipalSource;
    }

    @Bean
    @Order(AuthorizationServerConfiguration.AFTER_LOGIN_PAGE)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS)
                        .permitAll()    // for CORS preflight requests - OPTIONS must not be authorized
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer((oauth2) -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(klabisMemberEnhanceAuthentication())))
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(RequestCacheConfigurer::disable)
                .build();
    }

    // to enable spring security annotation templating like in @HasGrant
    @Bean
    static AnnotationTemplateExpressionDefaults prePostTemplateDefaults() {
        return new AnnotationTemplateExpressionDefaults();
    }

    @Bean
    public AbstractRequestLoggingFilter logFilter() {
        AbstractRequestLoggingFilter filter = new AbstractRequestLoggingFilter() {
            private static final Logger LOG = LoggerFactory.getLogger(ApisConfiguration.class);

            @Override
            protected boolean shouldLog(HttpServletRequest request) {
                return LOG.isDebugEnabled() && !request.getRequestURI().startsWith("/actuator");
            }

            @Override
            protected void beforeRequest(HttpServletRequest request, String message) {
                LOG.debug(message);
            }

            @Override
            protected void afterRequest(HttpServletRequest request, String message) {
                LOG.debug(message);
            }
        };
        filter.setIncludeQueryString(true);
        filter.setIncludeHeaders(false);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        //filter.setIncludeClientInfo(true);
        return filter;
    }

    // https://stackoverflow.com/questions/69100420/spring-oauth2-resource-server-load-synchronized-user-from-database

    private Converter<Jwt, KlabisUserAuthentication> klabisMemberEnhanceAuthentication() {
        return source -> klabisPrincipalSource.getPrincipalForUserName(source.getSubject())
                .map(principal -> KlabisUserAuthentication.authenticated(principal, source))
                .orElseGet(() -> KlabisUserAuthentication.noUser(source));
    }


    @Bean
    static RoleHierarchy customizedRoleHierarchy() {
        RoleHierarchyImpl hierarchy = RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > %s
                ROLE_ADMIN > %s
                """.formatted(ApplicationGrant.MEMBERS_EDIT.name(), ApplicationGrant.MEMBERS_REGISTER.name()));
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
                "http://localhost:3000",
                "https://preview--orienter-club-hub.lovable.app",
                "https://*.lovableproject.com",
                "https://klabis.otakar.io",
                "https://toedter.github.io",
                // HAL+FORMS explorer demo page URI => https://toedter.github.io/hal-explorer/release/hal-explorer/#theme=Zephyr&httpOptions=true&allHttpMethodsForLinks=true&hkey0=Authorization&hval0=eyJraWQiOiJmZDQ5YjUzMS1kZWIyLTRkNmItYjZmMS1lMTkyYzIyMDc4NjAiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJaQk04MDAzIiwiYXVkIjoiZnJvbnRlbmQiLCJuYmYiOjE3NjM2NjAwNjksInNjb3BlIjpbIm9wZW5pZCIsInByb2ZpbGUiLCJlbWFpbCJdLCJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo4NDQzIiwiZXhwIjoxNzYzNjYzNjY5LCJpYXQiOjE3NjM2NjAwNjksImp0aSI6Ijg5YjdhZDZiLTM2YjAtNDBlZS1iMjRlLTk5MWM4YzRiZDRlNSJ9.XrnTZQ2qgoIMwMO3JOagt598cJ0xkuR0A1wCrK9oNmZZIyPzxq_ePfKZmqQwgzYQmVdeYrElBA_LCsbONLtnoI74zZfWmGPsmn7p5iwQ-vZod4LMHYv1s8gHoEUOn7H3qdBCRbxd57_oltsf9yCCZgi6-4Kby4aPR3t8_2G4OoidiHj842zXDDXG14vd6RIFmfF2JGqAzxkaCFkd3LVhMs4CUJi4eLnJQyYRGCZiV8kuRyCUjGCw7TGA8mtw1A932my8QAFzFVZOd662t1BtcMCgmjCt-AMzSTqvQLEUNY4iJIHij7BospeWsqTo_NeN2Bw8TRN-if4bHlbNHX7mDw&hkey1=Accept&hval1=application/prs.hal-forms+json,application/hal+json&uri=https://localhost:8443/events/1
                "https://wiki.zabiny.club",
                "https://klabis-api-docs.otakar.io"
        ));

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

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
