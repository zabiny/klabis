package club.klabis.adapters.api;

import club.klabis.config.authserver.AuthorizationServerConfiguration;
import club.klabis.domain.appusers.ApplicationGrant;
import club.klabis.domain.appusers.ApplicationUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyUtils;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;
import java.util.Map;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration(proxyBeanMethods = false)
public class ApiSecurityConfiguration {

    public static RequestMatcher API_ENDPOINTS_MATCHER = new MediaTypeRequestMatcher(MediaType.APPLICATION_JSON);

    public ApiSecurityConfiguration(ApplicationUserService applicationUserService) {
        this.applicationUserService = applicationUserService;
    }

    @Bean
    @Order(AuthorizationServerConfiguration.AFTER_LOGIN_PAGE)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(API_ENDPOINTS_MATCHER)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(klabisMemberEnhanceAuthentication())))
                .build();
    }


    // https://stackoverflow.com/questions/69100420/spring-oauth2-resource-server-load-synchronized-user-from-database

    private final ApplicationUserService applicationUserService;

    private Converter<Jwt, KlabisUserAuthentication> klabisMemberEnhanceAuthentication() {
        return source -> applicationUserService.getApplicationUserForUsername(source.getSubject())
                .map(user -> KlabisUserAuthentication.authenticated(user, source))
                .orElseGet(() -> KlabisUserAuthentication.noUser(source));
    }

    @Bean
    static RoleHierarchy customizedRoleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        String hierarchyDef = RoleHierarchyUtils.roleHierarchyFromMap(
                Map.of("ROLE_ADMIN", List.of(ApplicationGrant.MEMBERS_EDIT.name(), ApplicationGrant.MEMBERS_REGISTER.name()))
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
}
