package club.klabis.adapters.api;

import club.klabis.config.authserver.AuthorizationServerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class ApiSecurityConfiguration {

    @Bean
    @Order(value = AuthorizationServerConfiguration.AFTER_AUTH_SERVER_SECURITY_ORDER)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(new AntPathRequestMatcher("/api/**"))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

}
