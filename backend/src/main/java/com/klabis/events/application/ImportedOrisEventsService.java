package com.klabis.events.application;

import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
class ImportedOrisEventsService implements ImportedOrisEventsPort {

    private static final Logger log = LoggerFactory.getLogger(ImportedOrisEventsService.class);

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
                .flatMap(reference -> {
                    String externalId = reference.externalReference().externalId();
                    try {
                        return Stream.of(Integer.valueOf(externalId));
                    } catch (NumberFormatException e) {
                        log.warn("Skipping non-numeric ORIS external ID '{}'", externalId, e);
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toSet());
    }
}
