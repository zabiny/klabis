package com.klabis.calendar.infrastructure.security;

import com.klabis.calendar.application.IcalTokenPort;
import com.klabis.common.security.ResourceServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Registers the iCal token authentication filter and provider into the resource-server
 * filter chain via the {@link ResourceServerCustomizer} extension point.
 * <p>
 * The filter is placed before {@code BasicAuthenticationFilter} (which is before
 * {@code BearerTokenAuthenticationFilter}), ensuring iCal token auth runs first on
 * {@code /ical/**} requests carrying a {@code ?token=} parameter.
 */
@Configuration
class CalendarSecurityConfiguration {

    @Bean
    ResourceServerCustomizer icalTokenResourceServerCustomizer(IcalTokenPort icalTokenPort) {
        return new IcalSecurityCustomizer(icalTokenPort);
    }

    private static class IcalSecurityCustomizer implements ResourceServerCustomizer {

        private final IcalTokenPort icalTokenPort;

        IcalSecurityCustomizer(IcalTokenPort icalTokenPort) {
            this.icalTokenPort = icalTokenPort;
        }

        @Override
        public void customize(HttpSecurity http) throws Exception {
            IcalTokenAuthenticationProvider provider = new IcalTokenAuthenticationProvider(icalTokenPort);
            AuthenticationManager authManager = new ProviderManager(provider);
            IcalTokenAuthenticationFilter filter = new IcalTokenAuthenticationFilter(authManager);
            http.addFilterBefore(filter, BasicAuthenticationFilter.class);
        }
    }
}
