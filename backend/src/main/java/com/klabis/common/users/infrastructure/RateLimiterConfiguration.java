package com.klabis.common.users.infrastructure;

import com.klabis.common.ratelimit.PerKeyRateLimiter;
import com.klabis.common.users.application.PasswordSetupProperties;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class RateLimiterConfiguration {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS));
        return manager;
    }

    @Bean
    public PerKeyRateLimiter passwordSetupRateLimiter(
            PasswordSetupProperties passwordSetupProperties,
            CacheManager cacheManager) {
        var rateLimit = passwordSetupProperties.getRateLimit();
        return new PerKeyRateLimiter(rateLimit.getRequests(), Duration.ofSeconds(rateLimit.getDurationSeconds()), cacheManager);
    }
}
