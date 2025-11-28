package club.klabis.shared.config.security;

import club.klabis.shared.config.authserver.AuthorizationServerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ErrorPageSecurity {
    @Bean("errorPageSecurityFilterChain")
    @Order(value = AuthorizationServerConfiguration.AFTER_AUTH_SERVER_SECURITY_ORDER)
    public SecurityFilterChain errorPageFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/error")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        c -> c.anyRequest().permitAll()
                );

        return http.build();
    }
}
