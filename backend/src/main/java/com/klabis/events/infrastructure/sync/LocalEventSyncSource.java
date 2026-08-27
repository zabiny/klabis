package com.klabis.events.infrastructure.sync;

import com.dpolach.api.orisclient.dto.EventDetails;
import com.klabis.common.sync.SyncId;
import com.klabis.common.sync.SyncParty;
import com.klabis.common.sync.SyncSource;
import com.klabis.common.sync.SyncType;
import com.klabis.events.EventId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventCreateEventFromOrisBuilder;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventSyncFromOrisBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Local ({@code party=LOCAL}) side of the ORIS event {@code SyncLine}. Owns the domain logic of
 * turning an ORIS payload into a Klabis {@link Event}: create-vs-update resolution, category merge
 * (inside {@link Event#syncFromOris}), the registration-removal warning, and auto event-type mapping.
 */
@Component
class LocalEventSyncSource implements SyncSource<EventSyncData> {

    private final EventRepository eventRepository;
    private final OrisEventDetailsMapper detailsMapper;
    private final OrisEventMappingSupport support;

    LocalEventSyncSource(EventRepository eventRepository,
                         OrisEventDetailsMapper detailsMapper,
                         OrisEventMappingSupport support) {
        this.eventRepository = eventRepository;
        this.detailsMapper = detailsMapper;
        this.support = support;
    }

    @Override
    public Optional<EventSyncData> fetch(SyncId syncId) {
        EventId id = new EventId(UUID.fromString(syncId.idValue()));
        return eventRepository.findById(id)
                .map(e -> new EventSyncData(e.getId(), e.getOrisId(), null, null));
    }

    @Override
    @Transactional
    public SyncId save(EventSyncData data) {
        Optional<Event> existing = data.eventId() != null
                ? eventRepository.findById(data.eventId())
                : eventRepository.findByOrisId(data.orisId());

        Event event;
        if (existing.isPresent()) {
            event = existing.get();
            List<com.klabis.events.EventCategory> incoming = support.extractCategories(data.orisDetails());
            support.warnIfSyncRemovesCategoriesWithRegistrations(event, incoming);
            event.syncFromOris(buildSyncCommand(data));
        } else {
            event = Event.createFromOris(buildCreateCommand(data));
        }

        event.applyAutoMappedEventType(support.resolveEventType(data.orisDetails().discipline()));

        Event saved = eventRepository.save(event);
        return SyncId.localId(SyncType.EVENT, saved.getId().value().toString());
    }

    @Override
    public SyncType type() {
        return SyncType.EVENT;
    }

    @Override
    public SyncParty party() {
        return SyncParty.LOCAL;
    }

    private Event.CreateEventFromOris buildCreateCommand(EventSyncData data) {
        EventDetails details = data.orisDetails();
        OrisEventDetailsMapper.TrivialEventFields trivial = detailsMapper.toTrivialFields(details);
        return EventCreateEventFromOrisBuilder.builder()
                .orisId(data.orisId())
                .name(trivial.name())
                .eventDate(trivial.eventDate())
                .location(trivial.location())
                .organizer(support.resolveOrganizer(details))
                .websiteUrl(WebsiteUrl.of(data.eventWebUrl()))
                .registrationDeadlines(support.buildRegistrationDeadlines(details, data.orisId()))
                .categories(support.extractCategories(details))
                .ranking(support.resolveRanking(details.level()))
                .baseEntryFee(support.deriveBaseEntryFee(details))
                .build();
    }

    private Event.SyncFromOris buildSyncCommand(EventSyncData data) {
        EventDetails details = data.orisDetails();
        OrisEventDetailsMapper.TrivialEventFields trivial = detailsMapper.toTrivialFields(details);
        return EventSyncFromOrisBuilder.builder()
                .name(trivial.name())
                .eventDate(trivial.eventDate())
                .location(trivial.location())
                .organizer(support.resolveOrganizer(details))
                .websiteUrl(WebsiteUrl.of(data.eventWebUrl()))
                .registrationDeadlines(support.buildRegistrationDeadlines(details, data.orisId()))
                .categories(support.extractCategories(details))
                .ranking(support.resolveRanking(details.level()))
                .baseEntryFee(support.deriveBaseEntryFee(details))
                .build();
    }
}
