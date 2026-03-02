package com.klabis.common.users.infrastructure;

import com.klabis.common.ratelimit.PerKeyRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for per-key rate limiting.
 *
 * <p>This configuration creates a PerKeyRateLimiter bean with settings
 * from application properties.
 */
@Configuration
@EnableCaching
public class RateLimiterConfiguration {

    /**
     * Creates a simple cache manager for rate limiting.
     *
     * <p>In production, consider using Redis for distributed rate limiting.
     *
     * @return cache manager with rate limit cache
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("rateLimitCache");
    }

    /**
     * Creates a PerKeyRateLimiter bean for password setup token requests.
     *
     * <p>Configuration from application.yml:
     * <ul>
     *   <li>password-setup.rate-limit.requests: Maximum requests per time window (default: 3)</li>
     *   <li>password-setup.rate-limit.duration-seconds: Time window in seconds (default: 3600 = 1 hour)</li>
     * </ul>
     *
     * @param limit           maximum requests per time window per key
     * @param durationSeconds time window duration in seconds
     * @param cacheManager    Spring cache manager
     * @return configured PerKeyRateLimiter
     */
    @Bean
    public PerKeyRateLimiter passwordSetupRateLimiter(
            @Value("${password-setup.rate-limit.requests:3}") int limit,
            @Value("${password-setup.rate-limit.duration-seconds:3600}") long durationSeconds,
            CacheManager cacheManager) {
        return new PerKeyRateLimiter(limit, Duration.ofSeconds(durationSeconds), cacheManager);
    }
}
