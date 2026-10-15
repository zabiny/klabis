package com.klabis.events.infrastructure.orissync;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("oris")
class OrisEventEntriesCacheConfiguration {

    @Bean
    SmartInitializingSingleton orisEventEntriesCacheRegistration(CacheManager cacheManager,
                                                                 OrisEntriesProperties properties) {
        return () -> {
            if (!(cacheManager instanceof CaffeineCacheManager caffeineCacheManager)) {
                throw new IllegalStateException("ORIS event entries cache needs a CaffeineCacheManager, found "
                        + cacheManager.getClass().getName());
            }
            caffeineCacheManager.registerCustomCache(OrisEventEntries.CACHE_NAME,
                    Caffeine.newBuilder().expireAfterWrite(properties.getOrisEntriesCacheTtl()).build());
        };
    }
}
