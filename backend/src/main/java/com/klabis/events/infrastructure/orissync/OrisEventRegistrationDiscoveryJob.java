package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.dto.EventEntry;
import com.klabis.common.OrisIntegrationComponent;
import com.klabis.events.EventId;
import com.klabis.events.application.OrisEventRegistrationDiscoveryPort;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.OrisRegistrationId;
import com.klabis.sync.application.SyncPairingLookup;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncedEntityReference;
import org.jmolecules.architecture.hexagonal.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Brings in the ORIS entries of club members for every ACTIVE event kept in step with ORIS.
 * <p>
 * Per event: read the club's entries, keep those of people paired as MEMBER/ORIS (entries without a
 * {@code ClubUserID} or of unknown people are ignored), subtract the participations already paired - any status,
 * a pairing is for life - and {@code pullAndEnroll} the rest. Already-paired registrations are kept in step by the
 * engine's own scans, never by this job, so it does not mark them dirty.
 */
@OrisIntegrationComponent
@Application
class OrisEventRegistrationDiscoveryJob implements OrisEventRegistrationDiscoveryPort {

    private static final Logger log = LoggerFactory.getLogger(OrisEventRegistrationDiscoveryJob.class);

    private final EventRepository eventRepository;
    private final OrisEventEntries orisEventEntries;
    private final SyncPairingLookup pairingLookup;
    private final SynchronizationPort synchronizationPort;

    OrisEventRegistrationDiscoveryJob(EventRepository eventRepository, OrisEventEntries orisEventEntries,
                                      SyncPairingLookup pairingLookup, SynchronizationPort synchronizationPort) {
        this.eventRepository = eventRepository;
        this.orisEventEntries = orisEventEntries;
        this.pairingLookup = pairingLookup;
        this.synchronizationPort = synchronizationPort;
    }

    @Scheduled(initialDelayString = "PT30S")
    void onStartup() {
        discover();
    }

    @Scheduled(cron = "${klabis.events.oris-entries-discovery-cron}")
    void discover() {
        log.info("Starting ORIS event registration discovery");

        List<SyncRecord> orisEvents = pairingLookup.findActiveByEntityType(SyncEntityType.EVENT).stream()
                .filter(record -> record.getExternalReference().system() == ExternalSystem.ORIS)
                .toList();
        Set<EventId> activeEvents = Set.copyOf(eventRepository.findActiveIds(orisEvents.stream()
                .map(record -> new EventId(UUID.fromString(record.getTarget().entityId())))
                .toList()));

        int discovered = 0;
        for (SyncRecord record : orisEvents) {
            if (!activeEvents.contains(new EventId(UUID.fromString(record.getTarget().entityId())))) {
                continue;
            }
            try {
                discovered += discoverFor(record.getExternalReference().externalId(), null);
            } catch (RuntimeException e) {
                log.error("Failed to discover ORIS registrations of event {}: {}",
                        record.getTarget().entityId(), e.getMessage(), e);
            }
        }

        log.info("ORIS event registration discovery completed: {} new registration(s) discovered", discovered);
    }

    /**
     * Discovery for one event on a user's request: reads ORIS afresh and passes {@code actingUser} to the engine.
     * Does nothing for an event that is not ACTIVE or not kept in step with ORIS.
     *
     * @return the number of registrations brought in
     */
    @Override
    public int discoverForEvent(EventId eventId, String actingUser) {
        if (eventRepository.findActiveIds(List.of(eventId)).isEmpty()) {
            return 0;
        }
        String entityId = eventId.value().toString();
        return pairingLookup.findActiveByTargets(SyncEntityType.EVENT, List.of(entityId)).stream()
                .filter(reference -> reference.externalReference().system() == ExternalSystem.ORIS)
                .findFirst()
                .map(reference -> {
                    orisEventEntries.evict(Integer.parseInt(reference.externalReference().externalId()));
                    return discoverFor(reference.externalReference().externalId(), actingUser);
                })
                .orElse(0);
    }

    private int discoverFor(String orisEventId, String actingUser) {
        List<String> clubUserIds = orisEventEntries.entriesOf(Integer.parseInt(orisEventId)).stream()
                .map(EventEntry::clubUserId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .distinct()
                .toList();
        if (clubUserIds.isEmpty()) {
            return 0;
        }

        Set<String> knownMembers = externalIdsOf(
                pairingLookup.findByExternalReferences(SyncEntityType.MEMBER, ExternalSystem.ORIS, clubUserIds));
        List<String> candidates = clubUserIds.stream()
                .filter(knownMembers::contains)
                .map(clubUserId -> new OrisRegistrationId(Integer.parseInt(orisEventId), Integer.parseInt(clubUserId)).format())
                .toList();
        if (candidates.isEmpty()) {
            return 0;
        }

        Set<String> alreadyPaired = externalIdsOf(
                pairingLookup.findByExternalReferences(SyncEntityType.EVENT_REGISTRATION, ExternalSystem.ORIS, candidates));

        int discovered = 0;
        for (String externalId : candidates) {
            if (alreadyPaired.contains(externalId)) {
                continue;
            }
            try {
                synchronizationPort.pullAndEnroll(SyncEntityType.EVENT_REGISTRATION,
                        new ExternalReference(ExternalSystem.ORIS, externalId), actingUser);
                discovered++;
            } catch (RuntimeException e) {
                log.error("Failed to discover ORIS registration {}: {}", externalId, e.getMessage(), e);
            }
        }
        return discovered;
    }

    private static Set<String> externalIdsOf(List<SyncedEntityReference> references) {
        return references.stream()
                .map(reference -> reference.externalReference().externalId())
                .collect(Collectors.toSet());
    }
}
