package com.klabis.common.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * Extension point for modules that need to contribute additional filters or configuration
 * to the resource-server {@link org.springframework.security.web.SecurityFilterChain}.
 * <p>
 * Implement this interface as a Spring {@code @Bean} to register module-specific
 * authentication filters (e.g. iCal token authentication) before the standard
 * bearer-token processing.
 * <p>
 * Modeled after {@link AuthorizationServerCustomizer} — collected via
 * {@code ObjectProvider<ResourceServerCustomizer>} in {@link WebSecurityCommonConfiguration}.
 */
public interface ResourceServerCustomizer {

    ResourceServerCustomizer EMPTY = http -> {};

    /**
     * Applies module-specific customizations to the resource-server HTTP security.
     * Called once during filter chain construction.
     */
    void customize(HttpSecurity http) throws Exception;
}
