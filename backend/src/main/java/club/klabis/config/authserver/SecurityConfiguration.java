package club.klabis.config.authserver;

import club.klabis.config.authserver.sociallogin.SocialLoginAuthenticationSuccessHandler;
import club.klabis.config.authserver.sociallogin.RegisterMemberFromSocialLoginHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    @Bean
    @Order(value = AuthorizationServerConfiguration.AFTER_AUTH_SERVER_SECURITY_ORDER)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                          AuthenticationSuccessHandler authenticationSuccessHandler
    ) throws Exception {
        return http
                .securityMatcher(new NegatedRequestMatcher(new AntPathRequestMatcher("/api/**")))
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
