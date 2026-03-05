package com.klabis.common.users.infrastructure;

import com.klabis.common.ratelimit.PerKeyRateLimiter;
import com.klabis.common.users.application.PasswordSetupProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class RateLimiterConfiguration {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("rateLimitCache");
    }

    @Bean
    public PerKeyRateLimiter passwordSetupRateLimiter(
            PasswordSetupProperties passwordSetupProperties,
            CacheManager cacheManager) {
        var rateLimit = passwordSetupProperties.getRateLimit();
        return new PerKeyRateLimiter(rateLimit.getRequests(), Duration.ofSeconds(rateLimit.getDurationSeconds()), cacheManager);
    }
}
