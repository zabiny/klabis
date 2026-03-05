package com.klabis.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS configuration for frontend access.
 * <p>
 * Externalized to environment variables for flexible configuration across environments.
 * <p>
 * Environment variables:
 * - FRONTEND_ALLOWED_ORIGINS: Comma-separated list of allowed origins (default: http://localhost:3000)
 * - FRONTEND_ALLOW_CREDENTIALS: Whether to allow credentials (default: true)
 */
@Configuration
public class CorsConfiguration {

    @Value("${frontend.allowed-origins:http://localhost:3000}")
    private String[] allowedOrigins;

    @Value("${frontend.allow-credentials:true}")
    private boolean allowCredentials;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowCredentials(allowCredentials);
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.addExposedHeader("Authorization");
        configuration.addExposedHeader("Location");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
