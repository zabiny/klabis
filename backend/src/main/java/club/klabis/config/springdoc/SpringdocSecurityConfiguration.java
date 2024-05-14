package club.klabis.config.springdoc;

import club.klabis.config.authserver.AuthorizationServerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SpringdocSecurityConfiguration {

    @Bean("swaggerDocChain")
    @Order(value = AuthorizationServerConfiguration.BEFORE_AUTH_SERVER_SECURITY_ORDER)
    public SecurityFilterChain swaggerFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/swagger-ui/*", "/v3/api-docs/*", "/v3/api-docs", "/*.yaml")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        c -> c.anyRequest().permitAll()
                );

        return http.build();
    }

}
