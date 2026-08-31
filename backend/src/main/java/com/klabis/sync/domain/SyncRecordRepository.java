package com.klabis.sync.domain;

import com.klabis.sync.SyncRecordId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Optional;

@SecondaryPort
public interface SyncRecordRepository {

    SyncRecord save(SyncRecord record);

    Optional<SyncRecord> findById(SyncRecordId id);

    /**
     * A record is unique per (target, external system) — a Klabis entity could in
     * principle be enrolled against more than one external system, so the system is
     * required alongside the target.
     */
    Optional<SyncRecord> findByTargetAndSystem(SyncTarget target, ExternalSystem system);
}
