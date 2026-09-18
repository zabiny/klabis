package com.klabis.sync.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SyncCapabilitiesTest {

    /**
     * design.md D3/"Domain Changes": what ORIS events declares for pull-and-enrol —
     * both sides read, only the local side written, the local side (and only the
     * local side) creatable.
     */
    @Test
    void pullOnlyCreating_declaresReadBothWriteLocalCreateLocalOnly() {
        SyncCapabilities capabilities = SyncCapabilities.pullOnlyCreating();

        assertThat(capabilities.readsLocal()).isTrue();
        assertThat(capabilities.readsExternal()).isTrue();
        assertThat(capabilities.writesLocal()).isTrue();
        assertThat(capabilities.writesExternal()).isFalse();
        assertThat(capabilities.createsLocal()).isTrue();
        assertThat(capabilities.createsExternal()).isFalse();
        assertThat(capabilities.containsSensitiveData()).isFalse();
    }
}
