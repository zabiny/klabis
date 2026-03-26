package com.klabis.common.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter that tracks request counts per key.
 *
 * <p>This implementation uses Spring Cache to store rate limit counters
 * for each key (e.g., registration number). Each key has its own independent
 * rate limit that resets after the configured duration.
 *
 * <p>Rate limiting configuration:
 * <ul>
 *   <li>limit: Maximum number of requests allowed per time window</li>
 *   <li>duration: Time window before counter resets</li>
 * </ul>
 */
public class PerKeyRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(PerKeyRateLimiter.class);
    private static final String CACHE_NAME = "rateLimitCache";

    private final int limit;
    private final Duration duration;
    private final Cache cache;
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    /**
     * Creates a new per-key rate limiter.
     *
     * @param limit        maximum requests per time window per key
     * @param duration     time window before counter resets
     * @param cacheManager Spring cache manager
     */
    public PerKeyRateLimiter(int limit, Duration duration, CacheManager cacheManager) {
        Assert.isTrue(limit > 0, "Limit must be positive");
        Assert.notNull(duration, "Duration must not be null");
        Assert.notNull(cacheManager, "CacheManager must not be null");

        this.limit = limit;
        this.duration = duration;
        this.cache = cacheManager.getCache(CACHE_NAME);

        if (this.cache == null) {
            throw new IllegalStateException("Cache '" + CACHE_NAME + "' not found. " +
                                            "Please configure a cache with this name.");
        }

        log.info("Initialized PerKeyRateLimiter: limit={} per {}, cache={}",
                limit, duration, CACHE_NAME);
    }

    /**
     * Checks if a request is allowed for the given key.
     *
     * <p>If the request is allowed, the counter is incremented.
     * If the limit has been exceeded, a RateLimitExceededException is thrown.
     *
     * @param key the key to check (e.g., registration number)
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    public void checkLimit(String key) {
        Object lock = locks.computeIfAbsent(key, k -> new Object());

        synchronized (lock) {
            RequestCounter counter = cache.get(key, RequestCounter::new);

            if (counter.getCount() >= limit) {
                log.warn("Rate limit exceeded for key: {}", key);
                throw new RateLimitExceededException(
                        "Rate limit exceeded: %d requests per %s allowed".formatted(limit, duration)
                );
            }

            counter.increment();
            cache.put(key, counter);
            log.debug("Rate limit check passed for key: {} (count: {})", key, counter.getCount());
        }
    }

    /**
     * Resets the counter for a specific key.
     *
     * <p>This is useful for testing or administrative purposes.
     *
     * @param key the key to reset
     */
    public void reset(String key) {
        cache.evict(key);
        log.debug("Reset rate limit counter for key: {}", key);
    }

    public void resetAll() {
        cache.clear();
        log.debug("Reset rate limit counter for all keys");
    }

    /**
     * Gets the current count for a key.
     *
     * <p>This is useful for monitoring and testing.
     *
     * @param key the key to check
     * @return current request count for the key
     */
    public int getCount(String key) {
        Cache.ValueWrapper wrapper = cache.get(key);
        if (wrapper != null) {
            RequestCounter counter = (RequestCounter) wrapper.get();
            return counter != null ? counter.getCount() : 0;
        }
        return 0;
    }

    /**
     * Internal counter class that tracks request count within a time window.
     */
    public static class RequestCounter {
        private int count = 0;

        public int getCount() {
            return count;
        }

        public void increment() {
            count++;
        }
    }
}
