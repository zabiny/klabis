package club.klabis.shared.config.security;

import club.klabis.shared.config.authserver.AuthorizationServerConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ActuatorSecurity {
    @Bean("actuatorSecurityFilterChain")
    @Order(value = AuthorizationServerConfiguration.BEFORE_AUTH_SERVER_SECURITY_ORDER)
    public SecurityFilterChain errorPageFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(EndpointRequest.to("health", "prometheus"))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(RequestCacheConfigurer::disable)
                .authorizeHttpRequests(
                        c -> c.anyRequest().permitAll()
                );

        return http.build();
    }
}
