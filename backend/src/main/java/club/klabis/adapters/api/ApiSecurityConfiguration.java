package club.klabis.adapters.api;

import club.klabis.config.authserver.AuthorizationServerConfiguration;
import club.klabis.domain.appusers.ApplicationUser;
import club.klabis.domain.appusers.ApplicationUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;

@EnableWebSecurity
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

    private Converter<Jwt, KlabisAuthenticatedUser> klabisMemberEnhanceAuthentication() {
        return source -> applicationUserService.getApplicationUserForUsername(source.getSubject())
                .map(user -> KlabisAuthenticatedUser.authenticated(user, source))
                .orElseGet(() -> KlabisAuthenticatedUser.noUser(source));
    }

    static class KlabisAuthenticatedUser extends AbstractAuthenticationToken {

        private final ApplicationUser applicationUser;
        private final Jwt authentication;

        static KlabisAuthenticatedUser authenticated(ApplicationUser user, Jwt credentials) {
            return new KlabisAuthenticatedUser(user, credentials);
        }

        static KlabisAuthenticatedUser noUser(Jwt credentials) {
            return new KlabisAuthenticatedUser(null, credentials);
        }

        private KlabisAuthenticatedUser(ApplicationUser applicationUser, Jwt authentication) {
            super(List.of());   // TODO: authorities..
            setAuthenticated(applicationUser != null);
            this.applicationUser = applicationUser;
            this.authentication = authentication;
        }

        @Override
        public Jwt getCredentials() {
            return authentication;
        }

        @Override
        public ApplicationUser getPrincipal() {
            return applicationUser;
        }
    }

}
