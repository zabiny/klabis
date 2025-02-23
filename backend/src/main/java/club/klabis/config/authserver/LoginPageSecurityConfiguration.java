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

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class LoginPageSecurityConfiguration {

    public static final String CUSTOM_LOGIN_PAGE = "/auth/login";

    public static RequestMatcher LOGIN_REQUESTS_MATCHER = new OrRequestMatcher(
            AntPathRequestMatcher.antMatcher(CUSTOM_LOGIN_PAGE),
            AntPathRequestMatcher.antMatcher("/oauth2/**"),
            AntPathRequestMatcher.antMatcher("/logout"),
            AntPathRequestMatcher.antMatcher("/login/**"));

    @Bean
    @Order(AuthorizationServerConfiguration.AUTH_SERVER_LOGIN_PAGE)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                          AuthenticationSuccessHandler authenticationSuccessHandler
    ) throws Exception {
        return http
                .securityMatcher(LOGIN_REQUESTS_MATCHER)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(CUSTOM_LOGIN_PAGE)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .formLogin(form -> form.loginPage(CUSTOM_LOGIN_PAGE).successHandler(authenticationSuccessHandler))
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
