package club.klabis.config.authserver;

import club.klabis.config.authserver.sociallogin.SocialLoginAuthenticationSuccessHandler;
import club.klabis.config.authserver.sociallogin.RegisterMemberFromSocialLoginHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.*;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class LoginPageSecurityConfiguration {

    public static RequestMatcher UI_REQUESTS_MATCHER = new AndRequestMatcher(new MediaTypeRequestMatcher(MediaType.TEXT_HTML), new NegatedRequestMatcher(new MediaTypeRequestMatcher(MediaType.ALL)));

    @Bean
    @Order
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                          AuthenticationSuccessHandler authenticationSuccessHandler
    ) throws Exception {
        return http
                //.securityMatcher(UI_REQUESTS_MATCHER)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .formLogin(withDefaults())
                .oauth2Login(oauth -> oauth.successHandler(authenticationSuccessHandler))
                .logout(LogoutConfigurer::permitAll)
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
