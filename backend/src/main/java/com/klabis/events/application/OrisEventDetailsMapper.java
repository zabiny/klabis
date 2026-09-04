package com.klabis.events.application;

import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.EventClass;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.Level;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.EventCategory;
import com.klabis.events.EventTypeId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.domain.EventRanking;
import com.klabis.events.domain.Money;
import com.klabis.events.domain.RegistrationDeadlines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Maps an ORIS {@code EventDetails} response into {@link OrisEventFields} — the
 * ORIS-owned event fields, independent of event type resolution (which needs the
 * {@code EventTypeRepository} and stays a method on {@link OrisEventImportService}).
 * <p>
 * Extracted so the mapping exists exactly once and is reused by both
 * {@link OrisEventImportService} and {@code com.klabis.oris.sync.OrisEventSyncAdapter}
 * (design.md D2, D3).
 */
final class OrisEventDetailsMapper {

    private static final Logger log = LoggerFactory.getLogger(OrisEventDetailsMapper.class);

    private static final String UNKNOWN_ORGANIZER = "---";

    private OrisEventDetailsMapper() {
    }

    static OrisEventFields map(EventDetails details, int orisId, OrisWebUrls orisWebUrls, EventTypeId resolvedEventTypeId) {
        String organizer = resolveOrganizer(details);
        WebsiteUrl websiteUrl = WebsiteUrl.of(orisWebUrls.eventUrl(orisId));
        RegistrationDeadlines registrationDeadlines = buildRegistrationDeadlines(details, orisId);
        List<EventCategory> categories = extractCategories(details);
        EventRanking ranking = resolveRanking(details.level());
        Money baseEntryFee = deriveBaseEntryFee(details);

        return new OrisEventFields(
                details.name(),
                details.date(),
                details.place(),
                organizer,
                websiteUrl,
                registrationDeadlines,
                categories,
                ranking,
                baseEntryFee,
                resolvedEventTypeId
        );
    }

    private static RegistrationDeadlines buildRegistrationDeadlines(EventDetails details, int orisId) {
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

    private static String resolveOrganizer(EventDetails details) {
        if (details.org1() != null && details.org1().abbreviation() != null && !details.org1().abbreviation().isBlank()) {
            return details.org1().abbreviation();
        }
        if (details.org2() != null && details.org2().abbreviation() != null && !details.org2().abbreviation().isBlank()) {
            return details.org2().abbreviation();
        }
        return UNKNOWN_ORGANIZER;
    }

    private static List<EventCategory> extractCategories(EventDetails details) {
        if (details.classes() == null || details.classes().isEmpty()) {
            return List.of();
        }
        return details.classes().values().stream()
                .filter(c -> c.name() != null && !c.name().isBlank())
                .map(c -> EventCategory.createFromOris(c.id(), c.name()))
                .toList();
    }

    private static EventRanking resolveRanking(Level level) {
        if (level == null) {
            return null;
        }
        return EventRanking.of(level.id(), level.shortName(), level.nameCZ());
    }

    private static Money deriveBaseEntryFee(EventDetails details) {
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
}
