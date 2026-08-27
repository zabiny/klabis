package com.klabis.sync;

public enum SyncParty {
    LOCAL, EXTERNAL;

    boolean isOppositeOf(SyncParty other) {
        return !this.equals(other);
    }
}
