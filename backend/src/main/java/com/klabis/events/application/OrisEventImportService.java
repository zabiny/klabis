package com.klabis.events.application;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.EventClass;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.klabis.events.EventId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.domain.*;
import com.klabis.oris.OrisIntegrationComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@OrisIntegrationComponent
class OrisEventImportService implements OrisEventImportPort {

    private static final Logger log = LoggerFactory.getLogger(OrisEventImportService.class);

    private static final String UNKNOWN_ORGANIZER = "---";

    private final EventRepository eventRepository;
    private final OrisApiClient orisApiClient;
    private final OrisWebUrls orisWebUrls;

    OrisEventImportService(EventRepository eventRepository,
                           OrisApiClient orisApiClient,
                           OrisWebUrls orisWebUrls) {
        this.eventRepository = eventRepository;
        this.orisApiClient = orisApiClient;
        this.orisWebUrls = orisWebUrls;
    }

    @Transactional
    @Override
    public Event importEventFromOris(int orisId) {
        EventDetails details = orisApiClient.getEventDetails(orisId).payload()
                .orElseThrow(() -> new EventNotFoundException(orisId));

        String organizer = resolveOrganizer(details);
        WebsiteUrl websiteUrl = WebsiteUrl.of(orisWebUrls.eventUrl(orisId));
        LocalDate registrationDeadline = details.entryDate1() != null ? details.entryDate1().toLocalDate() : null;
        List<String> categories = extractCategories(details);

        Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                .orisId(orisId)
                .name(details.name())
                .eventDate(details.date())
                .location(details.place())
                .organizer(organizer)
                .websiteUrl(websiteUrl)
                .registrationDeadline(registrationDeadline)
                .categories(categories)
                .build());

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

        String organizer = resolveOrganizer(details);
        WebsiteUrl websiteUrl = WebsiteUrl.of(orisWebUrls.eventUrl(orisId));
        LocalDate registrationDeadline = details.entryDate1() != null ? details.entryDate1().toLocalDate() : null;
        List<String> categories = extractCategories(details);

        warnIfSyncRemovesCategoriesWithRegistrations(event, categories);

        event.syncFromOris(EventSyncFromOrisBuilder.builder()
                .name(details.name())
                .eventDate(details.date())
                .location(details.place())
                .organizer(organizer)
                .websiteUrl(websiteUrl)
                .registrationDeadline(registrationDeadline)
                .categories(categories)
                .build());

        eventRepository.save(event);
    }

    private String resolveOrganizer(EventDetails details) {
        if (details.org1() != null && details.org1().abbreviation() != null && !details.org1().abbreviation().isBlank()) {
            return details.org1().abbreviation();
        }
        if (details.org2() != null && details.org2().abbreviation() != null && !details.org2().abbreviation().isBlank()) {
            return details.org2().abbreviation();
        }
        return UNKNOWN_ORGANIZER;
    }

    private List<String> extractCategories(EventDetails details) {
        if (details.classes() == null || details.classes().isEmpty()) {
            return List.of();
        }
        return details.classes().values().stream()
                .filter(c -> c.name() != null && !c.name().isBlank())
                .map(EventClass::name)
                .toList();
    }

    private void warnIfSyncRemovesCategoriesWithRegistrations(Event event, List<String> incomingCategories) {
        if (event.getRegistrations().isEmpty()) {
            return;
        }
        Set<String> incoming = Set.copyOf(incomingCategories);
        Map<String, Long> affectedCounts = event.getRegistrations().stream()
                .filter(r -> r.category() != null && !incoming.contains(r.category()))
                .collect(Collectors.groupingBy(EventRegistration::category, Collectors.counting()));
        if (!affectedCounts.isEmpty()) {
            log.warn("ORIS sync for event {} will remove categories that have existing registrations: {}",
                    event.getId(), affectedCounts);
        }
    }
}
