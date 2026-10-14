package com.klabis.sync.application;

import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncRecordRepository;
import com.klabis.sync.domain.SyncedEntityReference;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
class SyncPairingLookupService implements SyncPairingLookup {

    private final SyncRecordRepository syncRecordRepository;

    SyncPairingLookupService(SyncRecordRepository syncRecordRepository) {
        this.syncRecordRepository = syncRecordRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<SyncedEntityReference> findByExternalReferences(SyncEntityType entityType, ExternalSystem system,
                                                                Collection<String> externalIds) {
        return syncRecordRepository.findByExternalReferences(entityType, system, externalIds);
    }

    @Transactional(readOnly = true)
    @Override
    public List<SyncedEntityReference> findActiveByTargets(SyncEntityType entityType, Collection<String> entityIds) {
        return syncRecordRepository.findActiveByTargets(entityType, entityIds);
    }

    @Transactional(readOnly = true)
    @Override
    public List<SyncRecord> findActiveByEntityType(SyncEntityType entityType) {
        return syncRecordRepository.findAllNonRetired().stream()
                .filter(record -> record.getTarget().entityType() == entityType)
                .toList();
    }
}
