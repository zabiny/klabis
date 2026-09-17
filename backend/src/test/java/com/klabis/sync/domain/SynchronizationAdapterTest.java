package com.klabis.sync.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * design.md "Domain Changes": {@code createLocal} must default to refusing, so an
 * integration that never declares {@code createsLocal} cannot silently gain the
 * ability to create local entities just by not overriding the method.
 */
class SynchronizationAdapterTest {

    @Test
    void createLocal_defaultImplementation_refuses() {
        SynchronizationAdapter adapter = new SynchronizationAdapter() {
            @Override
            public SyncEntityType entityType() {
                return SyncEntityType.EVENT;
            }

            @Override
            public ExternalSystem system() {
                return ExternalSystem.ORIS;
            }

            @Override
            public SyncCapabilities capabilities() {
                return SyncCapabilities.pullOnly();
            }

            @Override
            public Class<? extends SyncProjection> projectionType() {
                return TestSyncProjectionStub.class;
            }

            @Override
            public SyncProjection readLocal(String entityId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SyncProjection readExternal(String externalId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void applyToLocal(String entityId, SyncProjection projection) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void applyToExternal(String externalId, SyncProjection projection) {
                throw new UnsupportedOperationException();
            }
        };

        assertThatThrownBy(() -> adapter.createLocal(new TestSyncProjectionStub()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private record TestSyncProjectionStub() implements SyncProjection {
        @Override
        public SyncEntityType entityType() {
            return SyncEntityType.EVENT;
        }
    }
}
