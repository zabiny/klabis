package com.klabis.common.sync;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Optional;

@SecondaryPort
public interface SyncRecordRepository {

    Optional<SyncRecord> findById(SyncId id);

    SyncRecord save(SyncRecord record);

}
