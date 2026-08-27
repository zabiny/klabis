package com.klabis.events.infrastructure.sync;

import com.klabis.common.sync.SyncItemId;
import com.klabis.common.sync.SyncParty;
import com.klabis.common.sync.SyncSource;
import com.klabis.common.sync.SyncType;
import com.klabis.events.EventId;
import com.klabis.events.application.DuplicateOrisImportException;
import com.klabis.events.application.EventSyncIds;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventCreateEventFromOrisBuilder;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventSyncFromOrisBuilder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Local ({@code party=LOCAL}) side of the ORIS event {@code SyncLine}. Consumes the already-translated
 * {@link EventSyncData} and turns it into a Klabis {@link Event}: create-vs-update resolution, the
 * registration-removal warning (delegated to {@link CategoryRegistrationGuard}), the category merge
 * (inside {@link Event#syncFromOris}), and auto event-type mapping. No ORIS DTO knowledge here.
 */
@Component
class LocalEventSyncSource implements SyncSource<EventSyncData> {

    private final EventRepository eventRepository;
    private final CategoryRegistrationGuard categoryGuard;

    LocalEventSyncSource(EventRepository eventRepository, CategoryRegistrationGuard categoryGuard) {
        this.eventRepository = eventRepository;
        this.categoryGuard = categoryGuard;
    }

    @Override
    public Optional<EventSyncData> fetch(SyncItemId syncItemId) {
        EventId id = EventSyncIds.toEventId(syncItemId);
        return eventRepository.findById(id).map(this::toSyncData);
    }

    @Override
    @Transactional
    public SyncItemId save(EventSyncData data) {
        Optional<Event> existing = data.eventId() != null
                ? eventRepository.findById(data.eventId())
                : eventRepository.findByOrisId(data.orisId());

        Event event;
        if (existing.isPresent()) {
            event = existing.get();
            categoryGuard.warnIfSyncRemovesCategoriesWithRegistrations(event, data.categories());
            event.syncFromOris(buildSyncCommand(data));
        } else {
            event = Event.createFromOris(buildCreateCommand(data));
        }

        event.applyAutoMappedEventType(data.autoMappedEventType());

        try {
            return EventSyncIds.localSyncId(eventRepository.save(event).getId());
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateOrisImportException(data.orisId(), e);
        }
    }

    @Override
    public SyncType type() {
        return SyncType.EVENT;
    }

    @Override
    public SyncParty party() {
        return SyncParty.LOCAL;
    }

    private EventSyncData toSyncData(Event e) {
        return new EventSyncData(
                e.getId(),
                e.getOrisId() != null ? e.getOrisId() : 0,
                e.getName(),
                e.getEventDate(),
                e.getLocation(),
                e.getOrganizer(),
                e.getWebsiteUrl(),
                e.getRegistrationDeadlines(),
                e.getCategories(),
                e.getRanking(),
                e.getBaseEntryFee(),
                null); // push path is dead — forward converter throws first
    }

    private Event.CreateEventFromOris buildCreateCommand(EventSyncData d) {
        return EventCreateEventFromOrisBuilder.builder()
                .orisId(d.orisId())
                .name(d.name())
                .eventDate(d.eventDate())
                .location(d.location())
                .organizer(d.organizer())
                .websiteUrl(d.websiteUrl())
                .registrationDeadlines(d.registrationDeadlines())
                .categories(d.categories())
                .ranking(d.ranking())
                .baseEntryFee(d.baseEntryFee())
                .build();
    }

    private Event.SyncFromOris buildSyncCommand(EventSyncData d) {
        return EventSyncFromOrisBuilder.builder()
                .name(d.name())
                .eventDate(d.eventDate())
                .location(d.location())
                .organizer(d.organizer())
                .websiteUrl(d.websiteUrl())
                .registrationDeadlines(d.registrationDeadlines())
                .categories(d.categories())
                .ranking(d.ranking())
                .baseEntryFee(d.baseEntryFee())
                .build();
    }
}
