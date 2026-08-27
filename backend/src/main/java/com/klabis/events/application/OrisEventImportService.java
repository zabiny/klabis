package com.klabis.events.application;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.klabis.events.EventCategory;
import com.klabis.events.EventId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.domain.*;
import com.klabis.events.infrastructure.sync.OrisEventMappingSupport;
import com.klabis.oris.OrisIntegrationComponent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@OrisIntegrationComponent
class OrisEventImportService implements OrisEventImportPort {

    private final EventRepository eventRepository;
    private final OrisApiClient orisApiClient;
    private final OrisWebUrls orisWebUrls;
    private final OrisEventMappingSupport mappingSupport;

    OrisEventImportService(EventRepository eventRepository,
                           OrisApiClient orisApiClient,
                           OrisWebUrls orisWebUrls,
                           OrisEventMappingSupport mappingSupport) {
        this.eventRepository = eventRepository;
        this.orisApiClient = orisApiClient;
        this.orisWebUrls = orisWebUrls;
        this.mappingSupport = mappingSupport;
    }

    @Transactional
    @Override
    public Event importEventFromOris(int orisId) {
        EventDetails details = orisApiClient.getEventDetails(orisId).payload()
                .orElseThrow(() -> new EventNotFoundException(orisId));

        String organizer = mappingSupport.resolveOrganizer(details);
        WebsiteUrl websiteUrl = WebsiteUrl.of(orisWebUrls.eventUrl(orisId));
        RegistrationDeadlines registrationDeadlines = mappingSupport.buildRegistrationDeadlines(details, orisId);
        List<EventCategory> categories = mappingSupport.extractCategories(details);
        EventRanking ranking = mappingSupport.resolveRanking(details.level());
        Money baseEntryFee = mappingSupport.deriveBaseEntryFee(details);

        Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                .orisId(orisId)
                .name(details.name())
                .eventDate(details.date())
                .location(details.place())
                .organizer(organizer)
                .websiteUrl(websiteUrl)
                .registrationDeadlines(registrationDeadlines)
                .categories(categories)
                .ranking(ranking)
                .baseEntryFee(baseEntryFee)
                .build());

        event.applyAutoMappedEventType(mappingSupport.resolveEventType(details.discipline()));

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

        int orisId = event.getOrisId();
        EventDetails details = orisApiClient.getEventDetails(orisId).payload()
                .orElseThrow(() -> new EventNotFoundException(orisId));

        String organizer = mappingSupport.resolveOrganizer(details);
        WebsiteUrl websiteUrl = WebsiteUrl.of(orisWebUrls.eventUrl(orisId));
        RegistrationDeadlines registrationDeadlines = mappingSupport.buildRegistrationDeadlines(details, orisId);
        List<EventCategory> categories = mappingSupport.extractCategories(details);
        EventRanking ranking = mappingSupport.resolveRanking(details.level());
        Money baseEntryFee = mappingSupport.deriveBaseEntryFee(details);

        mappingSupport.warnIfSyncRemovesCategoriesWithRegistrations(event, categories);

        event.syncFromOris(EventSyncFromOrisBuilder.builder()
                .name(details.name())
                .eventDate(details.date())
                .location(details.place())
                .organizer(organizer)
                .websiteUrl(websiteUrl)
                .registrationDeadlines(registrationDeadlines)
                .categories(categories)
                .ranking(ranking)
                .baseEntryFee(baseEntryFee)
                .build());

        event.applyAutoMappedEventType(mappingSupport.resolveEventType(details.discipline()));

        eventRepository.save(event);
    }
}
