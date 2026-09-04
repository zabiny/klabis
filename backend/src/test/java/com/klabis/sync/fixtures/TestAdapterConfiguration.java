package com.klabis.sync.fixtures;

import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SynchronizationAdapter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Shared {@link TestSynchronizationAdapter} bean for engine integration tests that
 * need no ORIS involvement (design.md Migration Plan, step 1) — a single {@code
 * @Import} target instead of the same nested {@code @TestConfiguration} repeated in
 * every test class.
 */
@TestConfiguration
public class TestAdapterConfiguration {

    @Bean
    SynchronizationAdapter testSynchronizationAdapter() {
        return new TestSynchronizationAdapter(SyncEntityType.EVENT, ExternalSystem.ORIS);
    }
}
