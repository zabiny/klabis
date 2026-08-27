package com.klabis.sync;

import java.util.Optional;

public interface SyncRecords {

    Optional<SyncRecord> findById(SyncId id);

}
