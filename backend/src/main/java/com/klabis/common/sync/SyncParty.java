package com.klabis.common.sync;

/**
 * Identifies party ("side") of the synchronization.
 */
public enum SyncParty {
    LOCAL, EXTERNAL;

    boolean isOppositeOf(SyncParty other) {
        return !this.equals(other);
    }
}
