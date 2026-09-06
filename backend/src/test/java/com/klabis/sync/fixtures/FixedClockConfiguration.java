package com.klabis.sync.fixtures;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.time.ZoneId;

/**
 * A fixed, advanceable {@link MutableClock} bean for engine tests that need to simulate the
 * passage of time without touching storage — a single {@code @Import} target instead
 * of the same nested {@code @TestConfiguration} repeated in every such test class.
 * <p>
 * Registered under its own name ({@code fixedClock}) and marked {@link Primary}, so it
 * sits alongside the production {@code com.klabis.common.ClockConfiguration} bean rather
 * than overriding it: an {@code @Autowired java.time.Clock} injection resolves to this
 * one, and a test that needs to advance time can {@code @Autowired MutableClock}
 * directly.
 */
@TestConfiguration
public class FixedClockConfiguration {

    /** The instant every {@link MutableClock} from this configuration starts at. */
    public static final Instant FIXED_NOW = Instant.parse("2026-06-01T00:00:00Z");

    @Bean
    @Primary
    MutableClock fixedClock() {
        return new MutableClock(FIXED_NOW, ZoneId.of("UTC"));
    }
}
