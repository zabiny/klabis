package com.klabis.sync.fixtures;

import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncProjection;

/**
 * A minimal, configurable projection for exercising the engine end-to-end without any
 * real integration (design.md Migration Plan, step 1; tasks.md 1.14).
 */
public record TestSyncProjection(String name, String value) implements SyncProjection {

    @Override
    public SyncEntityType entityType() {
        return SyncEntityType.EVENT;
    }
}
