package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.dto.lov.DisciplineListEntry;
import com.klabis.common.OrisIntegrationComponent;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncedEntityReference;
import org.jmolecules.architecture.hexagonal.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Discovers ORIS disciplines Klabis has never seen and enrols them for
 * synchronisation (design.md D4). The only mechanism able to bring in a
 * {@code Discipline} nobody has named explicitly: unlike an ORIS event, which is
 * always imported by an admin naming a specific ORIS id, nobody names "ORIS
 * discipline 7" — the engine has no built-in "list the external system and enrol
 * anything new" primitive, so this job supplies it for disciplines specifically.
 * <p>
 * Runs on its own {@code klabis.disciplines.discovery-cron} schedule
 * ({@link DisciplinesProperties}), independent of the sync engine's own
 * {@code klabis.sync.scan-cron}/{@code due-scan-interval} (design.md D4): disciplines
 * change far less often than events, and an already-discovered (paired) discipline is
 * kept current by the engine's existing scan regardless — this job only ever handles
 * ids it has never paired before.
 * <p>
 * Classified as {@link Application}, matching {@link DisciplineSyncAdapter}: this
 * class drives {@link OrisApiClient} (the ORIS integration's own client) while also
 * calling {@link SynchronizationPort}, {@code sync}'s primary port — the same
 * "adapter into one module, driving collaborator of another" shape (backend-patterns
 * skill, {@code synchronization-adapter.md}).
 */
@OrisIntegrationComponent
@Application
class DisciplineDiscoveryJob {

    private static final Logger log = LoggerFactory.getLogger(DisciplineDiscoveryJob.class);

    private final OrisApiClient orisApiClient;
    private final SynchronizationPort synchronizationPort;

    DisciplineDiscoveryJob(OrisApiClient orisApiClient, SynchronizationPort synchronizationPort) {
        this.orisApiClient = orisApiClient;
        this.synchronizationPort = synchronizationPort;
    }

    @Scheduled(cron = "${klabis.disciplines.discovery-cron}")
    void discoverNewDisciplines() {
        log.info("Starting ORIS discipline discovery");

        List<String> allExternalIds = orisApiClient.listDisciplines().payload()
                .map(DisciplineDiscoveryJob::externalIdsOf)
                .orElseGet(List::of);
        if (allExternalIds.isEmpty()) {
            log.info("ORIS discipline discovery completed: ORIS reported no disciplines");
            return;
        }

        Set<String> alreadyPaired = synchronizationPort
                .findByExternalReferences(SyncEntityType.DISCIPLINE, ExternalSystem.ORIS, allExternalIds)
                .stream()
                .map(reference -> reference.externalReference().externalId())
                .collect(Collectors.toSet());

        int discovered = 0;
        for (String externalId : allExternalIds) {
            if (alreadyPaired.contains(externalId)) {
                continue;
            }
            try {
                synchronizationPort.pullAndEnroll(
                        SyncEntityType.DISCIPLINE, new ExternalReference(ExternalSystem.ORIS, externalId), null);
                discovered++;
            } catch (RuntimeException e) {
                log.error("Failed to discover ORIS discipline {}: {}", externalId, e.getMessage(), e);
            }
        }

        log.info("ORIS discipline discovery completed: {} new discipline(s) discovered", discovered);
    }

    private static List<String> externalIdsOf(Map<String, DisciplineListEntry> disciplines) {
        return disciplines.values().stream()
                .map(DisciplineListEntry::id)
                .toList();
    }
}
