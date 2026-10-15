package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.autoconfigure.OrisClientProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;

@TestConfiguration
@EnableCaching
@Import(OrisEventEntriesCacheConfiguration.class)
class OrisEventEntriesTestConfig {

    static final int CLUB_ID = 205;

    @Bean
    CacheManager cacheManager() {
        return new CaffeineCacheManager();
    }

    @Bean
    OrisEntriesProperties orisEntriesProperties(@Value("${test.oris-entries-cache-ttl-millis}") long ttlMillis) {
        OrisEntriesProperties properties = new OrisEntriesProperties();
        properties.setOrisEntriesCacheTtl(Duration.ofMillis(ttlMillis));
        return properties;
    }

    @Bean
    OrisApiClient orisApiClient() {
        return org.mockito.Mockito.mock(OrisApiClient.class);
    }

    @Bean
    OrisClientProperties orisClientProperties() {
        return new OrisClientProperties(true, URI.create("https://oris.example"), URI.create("https://oris.example"),
                ZoneId.of("Europe/Prague"), "yyyy-MM-dd HH:mm:ss", CLUB_ID);
    }

    @Bean
    OrisEventEntries orisEventEntries(OrisApiClient orisApiClient, OrisClientProperties orisClientProperties) {
        return new OrisEventEntries(orisApiClient, orisClientProperties);
    }
}
