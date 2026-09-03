package com.klabis.events.application;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.Discipline;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.klabis.events.EventCategory;
import com.klabis.events.EventId;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.*;
import com.klabis.oris.OrisIntegrationComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@OrisIntegrationComponent
class OrisEventImportService implements OrisEventImportPort {

    private static final Logger log = LoggerFactory.getLogger(OrisEventImportService.class);

    private final EventRepository eventRepository;
    private final OrisApiClient orisApiClient;
    private final OrisWebUrls orisWebUrls;
    private final EventTypeRepository eventTypeRepository;

    OrisEventImportService(EventRepository eventRepository,
                           OrisApiClient orisApiClient,
                           OrisWebUrls orisWebUrls,
                           EventTypeRepository eventTypeRepository) {
        this.eventRepository = eventRepository;
        this.orisApiClient = orisApiClient;
        this.orisWebUrls = orisWebUrls;
        this.eventTypeRepository = eventTypeRepository;
    }

    @Transactional
    @Override
    public Event importEventFromOris(int orisId) {
        EventDetails details = fetchEventDetails(orisId);
        OrisEventFields fields = OrisEventDetailsMapper.map(details, orisId, orisWebUrls);

        Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                .orisId(orisId)
                .name(fields.name())
                .eventDate(fields.eventDate())
                .location(fields.location())
                .organizer(fields.organizer())
                .websiteUrl(fields.websiteUrl())
                .registrationDeadlines(fields.registrationDeadlines())
                .categories(fields.categories())
                .ranking(fields.ranking())
                .baseEntryFee(fields.baseEntryFee())
                .build());

        event.applyAutoMappedEventType(resolveEventTypeFromOrisDiscipline(details.discipline()));

        try {
            return eventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateOrisImportException(orisId);
        }
    }

    @Transactional
    @Override
    public void syncEventFromOris(EventId eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        OrisEventFields fields = readOrisFields(event.getOrisId());
        warnIfSyncRemovesCategoriesWithRegistrations(event, fields.categories());
        applyOrisSync(event, fields);
    }

    @Override
    @Transactional(readOnly = true)
    public OrisEventFields readOrisFields(int orisId) {
        EventDetails details = fetchEventDetails(orisId);
        OrisEventFields fields = OrisEventDetailsMapper.map(details, orisId, orisWebUrls);
        EventTypeId resolvedEventTypeId = resolveEventTypeFromOrisDiscipline(details.discipline());
        return new OrisEventFields(
                fields.name(), fields.eventDate(), fields.location(), fields.organizer(),
                fields.websiteUrl(), fields.registrationDeadlines(), fields.categories(),
                fields.ranking(), fields.baseEntryFee(), resolvedEventTypeId
        );
    }

    @Transactional
    @Override
    public Event applyOrisSync(EventId eventId, OrisEventFields fields) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        return applyOrisSync(event, fields);
    }

    private Event applyOrisSync(Event event, OrisEventFields fields) {
        event.syncFromOris(EventSyncFromOrisBuilder.builder()
                .name(fields.name())
                .eventDate(fields.eventDate())
                .location(fields.location())
                .organizer(fields.organizer())
                .websiteUrl(fields.websiteUrl())
                .registrationDeadlines(fields.registrationDeadlines())
                .categories(fields.categories())
                .ranking(fields.ranking())
                .baseEntryFee(fields.baseEntryFee())
                .build());

        event.applyAutoMappedEventType(fields.resolvedEventTypeId());

        return eventRepository.save(event);
    }

    private EventDetails fetchEventDetails(int orisId) {
        return orisApiClient.getEventDetails(orisId).payload()
                .orElseThrow(() -> new EventNotFoundException(orisId));
    }

    private EventTypeId resolveEventTypeFromOrisDiscipline(Discipline discipline) {
        if (discipline == null || discipline.id() <= 0) {
            // ORIS uses id 0 as sentinel for a missing discipline
            return null;
        }
        return eventTypeRepository.findByOrisDisciplineId(discipline.id())
                .map(EventType::getId)
                .orElse(null);
    }

    private void warnIfSyncRemovesCategoriesWithRegistrations(Event event, List<EventCategory> incomingCategories) {
        if (event.getRegistrations().isEmpty()) {
            return;
        }
        Set<String> incomingOrisIds = incomingCategories.stream()
                .map(EventCategory::orisId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, Long> affectedCounts = event.getRegistrations().stream()
                .filter(r -> r.categoryId() != null)
                .map(r -> event.findCategory(r.categoryId()).orElse(null))
                .filter(category -> category != null && category.orisId() != null && !incomingOrisIds.contains(category.orisId()))
                .collect(Collectors.groupingBy(EventCategory::name, Collectors.counting()));
        if (!affectedCounts.isEmpty()) {
            log.warn("ORIS sync for event {} will remove categories that have existing registrations: {}",
                    event.getId(), affectedCounts);
        }
    }
}
