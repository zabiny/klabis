package com.klabis.events.application;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.Discipline;
import com.dpolach.api.orisclient.dto.EventClass;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.Level;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.EventId;
import com.klabis.events.EventTypeId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.domain.*;
import com.klabis.oris.OrisIntegrationComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@OrisIntegrationComponent
class OrisEventImportService implements OrisEventImportPort {

    private static final Logger log = LoggerFactory.getLogger(OrisEventImportService.class);

    private static final String UNKNOWN_ORGANIZER = "---";
    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("CZK");

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
        EventDetails details = orisApiClient.getEventDetails(orisId).payload()
                .orElseThrow(() -> new EventNotFoundException(orisId));

        String organizer = resolveOrganizer(details);
        WebsiteUrl websiteUrl = WebsiteUrl.of(orisWebUrls.eventUrl(orisId));
        RegistrationDeadlines registrationDeadlines = buildRegistrationDeadlines(details, orisId);
        List<String> categories = extractCategories(details);
        EventRanking ranking = resolveRanking(details.level());
        Money baseEntryFee = deriveBaseEntryFee(details);

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

        int orisId = event.getOrisId();
        EventDetails details = orisApiClient.getEventDetails(orisId).payload()
                .orElseThrow(() -> new EventNotFoundException(orisId));

        String organizer = resolveOrganizer(details);
        WebsiteUrl websiteUrl = WebsiteUrl.of(orisWebUrls.eventUrl(orisId));
        RegistrationDeadlines registrationDeadlines = buildRegistrationDeadlines(details, orisId);
        List<String> categories = extractCategories(details);
        EventRanking ranking = resolveRanking(details.level());
        Money baseEntryFee = deriveBaseEntryFee(details);

        warnIfSyncRemovesCategoriesWithRegistrations(event, categories);

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

        event.applyAutoMappedEventType(resolveEventTypeFromOrisDiscipline(details.discipline()));

        eventRepository.save(event);
    }

    private RegistrationDeadlines buildRegistrationDeadlines(EventDetails details, int orisId) {
        LocalDate d1 = details.entryDate1() != null ? details.entryDate1().toLocalDate() : null;
        LocalDate d2 = details.entryDate2() != null ? details.entryDate2().toLocalDate() : null;
        LocalDate d3 = details.entryDate3() != null ? details.entryDate3().toLocalDate() : null;
        try {
            return RegistrationDeadlines.of(d1, d2, d3);
        } catch (IllegalArgumentException e) {
            log.error("ORIS event {} contains out-of-order or invalid registration deadlines (d1={}, d2={}, d3={}): {}",
                    orisId, d1, d2, d3, e.getMessage());
            throw new BusinessRuleViolationException(
                    "ORIS event %d has invalid registration deadlines: %s".formatted(orisId, e.getMessage())) {};
        }
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

    private EventTypeId resolveEventTypeFromOrisDiscipline(Discipline discipline) {
        if (discipline == null || discipline.id() <= 0) {
            // ORIS uses id 0 as sentinel for a missing discipline
            return null;
        }
        return eventTypeRepository.findByOrisDisciplineId(discipline.id())
                .map(EventType::getId)
                .orElse(null);
    }

    private EventRanking resolveRanking(Level level) {
        if (level == null) {
            return null;
        }
        return EventRanking.of(level.id(), level.shortName(), level.nameCZ());
    }

    private Money deriveBaseEntryFee(EventDetails details) {
        if (details.classes() == null || details.classes().isEmpty()) {
            return null;
        }
        Currency currency = resolveCurrency(details.currency());
        return details.classes().values().stream()
                .map(EventClass::fee)
                .filter(fee -> fee != null && !fee.isBlank())
                .map(fee -> {
                    try {
                        return new BigDecimal(fee.trim());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .map(maxFee -> Money.of(maxFee, currency))
                .orElse(null);
    }

    private Currency resolveCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        try {
            return Currency.getInstance(currencyCode.trim());
        } catch (IllegalArgumentException e) {
            return DEFAULT_CURRENCY;
        }
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
