package com.klabis.sync.infrastructure.jdbc;

import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncProjectionHasher;
import com.klabis.sync.domain.SyncProjectionType;
import com.klabis.sync.fixtures.TestSyncProjection;
import com.klabis.sync.infrastructure.SyncProjectionCodec;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test-only {@link SyncProjectionType} and {@link SyncProjectionHasher}, resolving
 * every entity type to a single test projection shape — sufficient for persistence
 * round-trip tests in this slice, which only ever exercise
 * {@link SyncEntityType#EVENT}. The {@code @DataJdbcTest} slice restricts component
 * scanning to {@code @Repository}-annotated beans, so these are provided explicitly
 * rather than relying on the production {@code @Component} adapters. Reuses
 * {@link TestSyncProjection} rather than declaring its own structurally identical
 * record.
 */
@TestConfiguration
class SyncProjectionTypeTestConfiguration {

    @Bean
    SyncProjectionType syncProjectionType() {
        return entityType -> TestSyncProjection.class;
    }

    @Bean
    SyncProjectionHasher syncProjectionHasher() {
        return SyncProjectionCodec::hash;
    }
}
