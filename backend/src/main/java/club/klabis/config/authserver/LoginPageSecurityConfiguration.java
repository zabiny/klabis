package club.klabis.config.authserver;

import club.klabis.config.authserver.sociallogin.RegisterMemberFromSocialLoginHandler;
import club.klabis.config.authserver.sociallogin.SocialLoginAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class LoginPageSecurityConfiguration {

    public static RequestMatcher LOGIN_REQUESTS_MATCHER = new OrRequestMatcher(AntPathRequestMatcher.antMatcher("/login"), AntPathRequestMatcher.antMatcher("/oauth2/**"), AntPathRequestMatcher.antMatcher("/logout"), AntPathRequestMatcher.antMatcher("/login/**"));

    @Bean
    @Order(AuthorizationServerConfiguration.AUTH_SERVER_LOGIN_PAGE)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                          AuthenticationSuccessHandler authenticationSuccessHandler
    ) throws Exception {
        return http
                .securityMatcher(LOGIN_REQUESTS_MATCHER)
                .formLogin(form -> form.successHandler(authenticationSuccessHandler))
                .oauth2Login(oauth -> oauth.successHandler(authenticationSuccessHandler))
                .build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler(RegisterMemberFromSocialLoginHandler handler) {
        SocialLoginAuthenticationSuccessHandler authenticationSuccessHandler =
                new SocialLoginAuthenticationSuccessHandler();
        authenticationSuccessHandler.setOidcUserHandler(handler);
        return authenticationSuccessHandler;
    }
}
