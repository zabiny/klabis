package com.klabis.events.application;

import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
class ImportedOrisEventsService implements ImportedOrisEventsPort {

    private final SynchronizationPort synchronizationPort;

    ImportedOrisEventsService(SynchronizationPort synchronizationPort) {
        this.synchronizationPort = synchronizationPort;
    }

    @Override
    public Set<Integer> findImportedOrisIds(Collection<Integer> candidateOrisIds) {
        if (candidateOrisIds.isEmpty()) {
            return Set.of();
        }
        Set<String> externalIds = candidateOrisIds.stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());
        return synchronizationPort.findByExternalReferences(SyncEntityType.EVENT, ExternalSystem.ORIS, externalIds)
                .stream()
                .map(reference -> Integer.valueOf(reference.externalReference().externalId()))
                .collect(Collectors.toSet());
    }
}
