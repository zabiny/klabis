package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.dto.EventEntry;
import com.klabis.common.OrisIntegrationComponent;
import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventStatus;
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
import java.util.Set;
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
class OrisEventRegistrationDiscoveryJob {

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

        int discovered = 0;
        for (SyncRecord record : pairingLookup.findActiveByEntityType(SyncEntityType.EVENT)) {
            if (record.getExternalReference().system() != ExternalSystem.ORIS) {
                continue;
            }
            try {
                discovered += discoverFor(record.getTarget().entityId(), record.getExternalReference().externalId(), null);
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
    int discoverForEvent(EventId eventId, String actingUser) {
        String entityId = eventId.value().toString();
        return pairingLookup.findActiveByTargets(SyncEntityType.EVENT, List.of(entityId)).stream()
                .filter(reference -> reference.externalReference().system() == ExternalSystem.ORIS)
                .findFirst()
                .map(reference -> {
                    orisEventEntries.evict(Integer.parseInt(reference.externalReference().externalId()));
                    return discoverFor(entityId, reference.externalReference().externalId(), actingUser);
                })
                .orElse(0);
    }

    private int discoverFor(String eventEntityId, String orisEventId, String actingUser) {
        Event event = eventRepository.findById(new EventId(java.util.UUID.fromString(eventEntityId))).orElse(null);
        if (event == null || event.getStatus() != EventStatus.ACTIVE) {
            return 0;
        }

        List<String> clubUserIds = orisEventEntries.entriesOf(Integer.parseInt(orisEventId)).stream()
                .map(EventEntry::clubUserId)
                .filter(java.util.Objects::nonNull)
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
                .map(clubUserId -> orisEventId + ":" + clubUserId)
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
