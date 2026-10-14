package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.autoconfigure.OrisClientProperties;
import com.dpolach.api.orisclient.dto.EventEntry;
import com.klabis.common.OrisIntegrationComponent;
import com.klabis.events.EventCategory;
import com.klabis.events.EventId;
import com.klabis.events.application.EventNotFoundException;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventSyncRegistrationFromOrisBuilder;
import com.klabis.events.domain.OrisCategoryNotYetSynchronisedException;
import com.klabis.events.domain.OrisRegistrationId;
import com.klabis.events.domain.RegistrationNote;
import com.klabis.events.domain.RegistrationSyncId;
import com.klabis.events.domain.SiCardNumber;
import com.klabis.members.MemberId;
import com.klabis.sync.application.SyncPairingLookup;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.ExternalVersionToken;
import com.klabis.sync.domain.RetryableSyncFailureException;
import com.klabis.sync.domain.SyncCapabilities;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncProjection;
import com.klabis.sync.domain.SyncTarget;
import com.klabis.sync.domain.SynchronizationAdapter;
import org.jmolecules.architecture.hexagonal.Application;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pairs one participation (a member on an event) with the member's entry in ORIS; ORIS to Klabis only.
 * <p>
 * Entity id is {@code "<eventId>:<memberId>"}, external id {@code "<orisEventId>:<clubUserId>"} (the ORIS
 * club-membership id of the member): both stable across deleting and re-creating the registration or the entry,
 * which an id of either record would not be. The registration is reached only through the {@link Event}
 * aggregate.
 * <p>
 * The EVENT and MEMBER pairings are resolved through {@link SyncPairingLookup} rather than
 * {@link SynchronizationPort}, which depends on every adapter. {@link SynchronizationPort} is injected
 * {@code @Lazy} for the single {@code markDirty} that asks the event's own synchronisation to bring in a category.
 * <p>
 * Classified as {@link Application}, like the other ORIS adapters: it implements {@code sync}'s secondary port and
 * calls a foreign primary port at once.
 */
@OrisIntegrationComponent
@Application
class OrisEventRegistrationSyncAdapter implements SynchronizationAdapter {

    private static final SyncCapabilities CAPABILITIES =
            new SyncCapabilities(true, true, true, false, true, false, true);

    private final EventRepository eventRepository;
    private final OrisEventEntries orisEventEntries;
    private final SyncPairingLookup pairingLookup;
    private final OrisClientProperties orisClientProperties;
    private final SynchronizationPort synchronizationPort;

    OrisEventRegistrationSyncAdapter(EventRepository eventRepository,
                                     OrisEventEntries orisEventEntries,
                                     SyncPairingLookup pairingLookup,
                                     OrisClientProperties orisClientProperties,
                                     @Lazy SynchronizationPort synchronizationPort) {
        this.eventRepository = eventRepository;
        this.orisEventEntries = orisEventEntries;
        this.pairingLookup = pairingLookup;
        this.orisClientProperties = orisClientProperties;
        this.synchronizationPort = synchronizationPort;
    }

    @Override
    public SyncEntityType entityType() {
        return SyncEntityType.EVENT_REGISTRATION;
    }

    @Override
    public ExternalSystem system() {
        return ExternalSystem.ORIS;
    }

    @Override
    public SyncCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public Class<? extends SyncProjection> projectionType() {
        return OrisEventRegistrationProjection.class;
    }

    @Override
    @Transactional(readOnly = true)
    public SyncProjection readLocal(String entityId) {
        RegistrationSyncId participation = RegistrationSyncId.parse(entityId);
        Event event = loadEvent(participation.eventId());
        return event.findRegistration(participation.memberId())
                .filter(registration -> !registration.missingInOris())
                .map(registration -> localProjection(event, registration))
                .orElseGet(OrisEventRegistrationProjection::absent);
    }

    private static OrisEventRegistrationProjection localProjection(Event event, EventRegistration registration) {
        String categoryOrisId = event.findCategory(registration.categoryId())
                .map(EventCategory::orisId)
                .orElse(null);
        return new OrisEventRegistrationProjection(
                true,
                categoryOrisId,
                registration.siCardNumber() != null ? registration.siCardNumber().value() : null,
                registration.wantsToRentSi(),
                RegistrationNote.textOrNull(registration.orisNote()),
                null, null);
    }

    @Override
    public SyncProjection readExternal(String externalId) {
        OrisRegistrationId orisId = OrisRegistrationId.parse(externalId);
        int orisEventId = orisId.orisEventId();
        int clubUserId = orisId.clubUserId();

        List<EventEntry> entries;
        try {
            entries = orisEventEntries.entriesOf(orisEventId);
        } catch (IllegalStateException e) {
            throw new RetryableSyncFailureException("ORIS did not answer the entries of event " + orisEventId, e);
        }

        return entries.stream()
                .filter(entry -> entry.clubUserId() != null && entry.clubUserId() == clubUserId)
                .findFirst()
                .map(entry -> externalProjection(entry, orisId))
                .orElseGet(OrisEventRegistrationProjection::absent);
    }

    private OrisEventRegistrationProjection externalProjection(EventEntry entry, OrisRegistrationId orisId) {
        return new OrisEventRegistrationProjection(
                true,
                entry.classId() != null ? String.valueOf(entry.classId()) : null,
                normalisedSiCard(entry.si()),
                Boolean.TRUE.equals(entry.rentSi()),
                RegistrationNote.textOrNull(RegistrationNote.truncatingOf(entry.note())),
                entry.createdDateTime() != null
                        ? entry.createdDateTime().atZone(orisClientProperties.timezone()).toInstant() : null,
                orisId);
    }

    private UUID resolveEntityId(SyncEntityType entityType, int externalId) {
        return pairingLookup.findByExternalReferences(entityType, ExternalSystem.ORIS, List.of(String.valueOf(externalId)))
                .stream()
                .findFirst()
                .map(reference -> UUID.fromString(reference.target().entityId()))
                .orElseThrow(() -> new IllegalStateException(
                        "No " + entityType + " is paired with ORIS id " + externalId));
    }

    private static String normalisedSiCard(Integer si) {
        if (si == null || si == 0) {
            return null;
        }
        try {
            return SiCardNumber.of(String.valueOf(si)).value();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public Optional<ExternalVersionToken> externalVersion(String externalId) {
        return Optional.empty();
    }

    @Override
    @Transactional
    public void applyToLocal(String entityId, SyncProjection projection) {
        RegistrationSyncId participation = RegistrationSyncId.parse(entityId);
        OrisEventRegistrationProjection orisProjection = (OrisEventRegistrationProjection) projection;
        syncRegistration(participation.eventId(), participation.memberId(), orisProjection);
    }

    @Override
    @Transactional
    public String createLocal(SyncProjection projection) {
        OrisEventRegistrationProjection orisProjection = (OrisEventRegistrationProjection) projection;
        RegistrationSyncId id = new RegistrationSyncId(
                new EventId(resolveEntityId(SyncEntityType.EVENT, orisProjection.orisId().orisEventId())),
                new MemberId(resolveEntityId(SyncEntityType.MEMBER, orisProjection.orisId().clubUserId())));
        syncRegistration(id.eventId(), id.memberId(), orisProjection);
        return id.format();
    }

    private void syncRegistration(EventId eventId, MemberId memberId, OrisEventRegistrationProjection projection) {
        Event event = loadEvent(eventId);
        try {
            if (projection.present()) {
                event.syncRegistrationFromOris(memberId, command(projection));
            } else {
                event.markRegistrationMissingInOris(memberId);
            }
        } catch (OrisCategoryNotYetSynchronisedException e) {
            synchronizationPort.markDirty(new SyncTarget(SyncEntityType.EVENT, eventId.value().toString()));
            throw e;
        }
        try {
            eventRepository.save(event);
        } catch (OptimisticLockingFailureException e) {
            throw new RetryableSyncFailureException("Event " + eventId + " was changed concurrently", e);
        }
    }

    private static Event.SyncRegistrationFromOris command(OrisEventRegistrationProjection projection) {
        return EventSyncRegistrationFromOrisBuilder.builder()
                .categoryOrisId(projection.categoryOrisId())
                .siCardNumber(projection.siCardNumber() != null ? SiCardNumber.of(projection.siCardNumber()) : null)
                .wantsToRentSi(projection.wantsToRentSi())
                .orisNote(RegistrationNote.ofNullable(projection.orisNote()))
                .registeredAt(projection.registeredAt())
                .build();
    }

    @Override
    public void applyToExternal(String externalId, SyncProjection projection) {
        throw new UnsupportedOperationException(
                "The ORIS event registration adapter declares no outward write capability");
    }

    private Event loadEvent(EventId eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
    }
}
