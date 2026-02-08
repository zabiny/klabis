package com.klabis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Spring Data JDBC configuration.
 * <p>
 * It provides:
 * - JDBC repository support
 * - Auditing support (createdDate, lastModifiedDate, createdBy, lastModifiedBy)
 * - Custom type converters for value objects
 * <p>
 * The configuration is disabled by default (JPA is the default persistence implementation).
 * To enable JDBC persistence, set the environment variable: PERSISTENCE_TYPE=jdbc
 */
@Configuration
@EnableJdbcRepositories(basePackages = {"com.klabis"})
@EnableJdbcAuditing
public class JdbcConfiguration extends AbstractJdbcConfiguration {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }

    /**
     * AuditorAware implementation that extracts username from SecurityContext.
     * Returns the authenticated user's registrationNumber, or "system" if not authenticated.
     */
    static class AuditorAwareImpl implements AuditorAware<String> {

        @Override
        public Optional<String> getCurrentAuditor() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return Optional.of(authentication.getName());
            }
            return Optional.of("system");
        }
    }

}
