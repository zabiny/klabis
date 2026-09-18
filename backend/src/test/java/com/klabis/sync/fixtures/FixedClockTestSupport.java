package com.klabis.sync.fixtures;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Shared constant and factory for the fixed, advanceable {@link MutableClock} used by
 * engine tests that simulate the passage of time without touching storage.
 * <p>
 * Not a Spring configuration: the two tests that need it replace the production
 * {@code java.time.Clock} bean with {@link #fixedClock()} via {@code @TestBean}
 * (Spring bean-override, type-deterministic, no
 * {@code spring.main.allow-bean-definition-overriding}). This class only removes the
 * copy-pasted constant and factory body from those tests.
 */
public final class FixedClockTestSupport {

    /** The instant every {@link MutableClock} from {@link #fixedClock()} starts at. */
    public static final Instant FIXED_NOW = Instant.parse("2026-06-01T00:00:00Z");

    private FixedClockTestSupport() {
    }

    /** A fresh {@link MutableClock} fixed at {@link #FIXED_NOW}, UTC. */
    public static MutableClock fixedClock() {
        return new MutableClock(FIXED_NOW, ZoneId.of("UTC"));
    }
}
