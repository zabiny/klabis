package com.klabis.events.infrastructure.sync;

import com.dpolach.api.orisclient.dto.Discipline;
import com.dpolach.api.orisclient.dto.EventClass;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.Level;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.EventCategory;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRanking;
import com.klabis.events.domain.EventType;
import com.klabis.events.domain.EventTypeRepository;
import com.klabis.events.domain.Money;
import com.klabis.events.domain.RegistrationDeadlines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business mapping helpers for turning an ORIS {@link EventDetails} payload into Klabis event data.
 * <p>
 * These are the non-trivial steps that cannot be expressed as plain field-to-field mapping:
 * organizer fallback, registration-deadline validation, category extraction, ranking, max entry fee,
 * and the ORIS-discipline to event-type lookup (which needs {@link EventTypeRepository}).
 */
@Component
public class OrisEventMappingSupport {

    private static final Logger log = LoggerFactory.getLogger(OrisEventMappingSupport.class);

    public static final String UNKNOWN_ORGANIZER = "---";

    private final EventTypeRepository eventTypeRepository;

    public OrisEventMappingSupport(EventTypeRepository eventTypeRepository) {
        this.eventTypeRepository = eventTypeRepository;
    }

    public RegistrationDeadlines buildRegistrationDeadlines(EventDetails details, int orisId) {
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

    public String resolveOrganizer(EventDetails details) {
        if (details.org1() != null && details.org1().abbreviation() != null && !details.org1().abbreviation().isBlank()) {
            return details.org1().abbreviation();
        }
        if (details.org2() != null && details.org2().abbreviation() != null && !details.org2().abbreviation().isBlank()) {
            return details.org2().abbreviation();
        }
        return UNKNOWN_ORGANIZER;
    }

    public List<EventCategory> extractCategories(EventDetails details) {
        if (details.classes() == null || details.classes().isEmpty()) {
            return List.of();
        }
        return details.classes().values().stream()
                .filter(c -> c.name() != null && !c.name().isBlank())
                .map(c -> EventCategory.createFromOris(c.id(), c.name()))
                .toList();
    }

    public EventTypeId resolveEventType(Discipline discipline) {
        if (discipline == null || discipline.id() <= 0) {
            // ORIS uses id 0 as sentinel for a missing discipline
            return null;
        }
        return eventTypeRepository.findByOrisDisciplineId(discipline.id())
                .map(EventType::getId)
                .orElse(null);
    }

    public EventRanking resolveRanking(Level level) {
        if (level == null) {
            return null;
        }
        return EventRanking.of(level.id(), level.shortName(), level.nameCZ());
    }

    public Money deriveBaseEntryFee(EventDetails details) {
        if (details.classes() == null || details.classes().isEmpty()) {
            return null;
        }
        Currency currency = Money.parseCurrency(details.currency());
        return details.classes().values().stream()
                .map(EventClass::fee)
                .filter(fee -> fee != null && !fee.isBlank())
                .map(fee -> {
                    try {
                        return new BigDecimal(fee.trim());
                    } catch (NumberFormatException e) {
                        return (BigDecimal) null;
                    }
                })
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .map(maxFee -> Money.of(maxFee, currency))
                .orElse(null);
    }

    public void warnIfSyncRemovesCategoriesWithRegistrations(Event event, List<EventCategory> incomingCategories) {
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
