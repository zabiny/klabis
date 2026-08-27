package com.klabis.sync;

public interface SyncData {

    long checksum();

    SyncId getSyncId();
}
