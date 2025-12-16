package club.klabis.shared.config.frontend;

import club.klabis.shared.config.authserver.AuthorizationServerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class FrontendSecurityConfiguration {

    @Bean("frontedDistChain")
    @Order(value = AuthorizationServerConfiguration.AFTER_AUTH_SERVER_SECURITY_ORDER)
    public SecurityFilterChain frontedFilterChain(HttpSecurity http) {
        http.securityMatcher("/index.*", "/assets/*", "/auth/callback")
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        c -> c.anyRequest().permitAll()
                );

        return http.build();
    }

}
