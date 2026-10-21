package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.Discipline;
import com.dpolach.api.orisclient.dto.EventClass;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.Level;
import com.klabis.common.OrisIntegrationComponent;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.DisciplineId;
import com.klabis.events.EventTypeId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.application.EventNotFoundException;
import com.klabis.events.domain.EventRanking;
import com.klabis.events.domain.EventType;
import com.klabis.events.domain.EventTypeRepository;
import com.klabis.events.domain.Money;
import com.klabis.events.domain.RegistrationDeadlines;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncedEntityReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads an ORIS event and maps it to the external-side {@link OrisEventProjection}.
 * <p>
 * Deliberately NOT {@code @Transactional}: the synchronisation engine calls
 * {@link OrisEventSyncAdapter#readExternal} with no transaction open, and wrapping this
 * would hold one around the blocking ORIS call.
 * <p>
 * {@link SynchronizationPort} is injected {@code @Lazy} to break the bean cycle
 * {@code OrisEventDetailsReader -> SynchronizationPort -> SynchronizationAdapterRegistry
 * -> OrisEventSyncAdapter -> OrisEventDetailsReader}.
 */
@OrisIntegrationComponent
class OrisEventDetailsReader {

    private static final Logger log = LoggerFactory.getLogger(OrisEventDetailsReader.class);

    private static final String UNKNOWN_ORGANIZER = "---";

    private final OrisApiClient orisApiClient;
    private final OrisWebUrls orisWebUrls;
    private final EventTypeRepository eventTypeRepository;
    private final SynchronizationPort synchronizationPort;

    OrisEventDetailsReader(OrisApiClient orisApiClient,
                           OrisWebUrls orisWebUrls,
                           EventTypeRepository eventTypeRepository,
                           @Lazy SynchronizationPort synchronizationPort) {
        this.orisApiClient = orisApiClient;
        this.orisWebUrls = orisWebUrls;
        this.eventTypeRepository = eventTypeRepository;
        this.synchronizationPort = synchronizationPort;
    }

    OrisEventProjection read(int orisId) {
        EventDetails details = orisApiClient.getEventDetails(orisId).payload()
                .orElseThrow(() -> new EventNotFoundException(orisId));
        EventTypeId resolvedEventTypeId = resolveEventType(details.discipline());
        return toProjection(details, orisId, orisWebUrls, resolvedEventTypeId);
    }

    static OrisEventProjection toProjection(EventDetails details, int orisId, OrisWebUrls orisWebUrls,
                                            EventTypeId resolvedEventTypeId) {
        WebsiteUrl websiteUrl = WebsiteUrl.of(orisWebUrls.eventUrl(orisId));
        RegistrationDeadlines deadlines = buildRegistrationDeadlines(details, orisId);
        EventRanking ranking = resolveRanking(details.level());
        Money baseEntryFee = deriveBaseEntryFee(details);

        return new OrisEventProjection(
                details.name(),
                details.date(),
                details.place(),
                resolveOrganizer(details),
                websiteUrl.value(),
                deadlines.deadline1().orElse(null),
                deadlines.deadline2().orElse(null),
                deadlines.deadline3().orElse(null),
                extractCategories(details),
                ranking != null ? ranking.levelId() : null,
                ranking != null ? ranking.shortName() : null,
                ranking != null ? ranking.name() : null,
                baseEntryFee != null ? baseEntryFee.amount() : null,
                baseEntryFee != null ? baseEntryFee.currency().getCurrencyCode() : null,
                resolvedEventTypeId,
                orisId
        );
    }

    private EventTypeId resolveEventType(Discipline discipline) {
        if (discipline == null || discipline.id() <= 0) {
            // ORIS uses id 0 as sentinel for a missing discipline
            return null;
        }
        return findPairedDisciplineId(discipline.id())
                .flatMap(eventTypeRepository::findByDisciplineId)
                .map(EventType::getId)
                .orElse(null);
    }

    private Optional<DisciplineId> findPairedDisciplineId(int orisDisciplineId) {
        List<SyncedEntityReference> pairings = synchronizationPort.findByExternalReferences(
                SyncEntityType.DISCIPLINE, ExternalSystem.ORIS, List.of(String.valueOf(orisDisciplineId)));
        return pairings.stream()
                .findFirst()
                .map(reference -> new DisciplineId(UUID.fromString(reference.target().entityId())));
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

    private static List<OrisEventProjection.Category> extractCategories(EventDetails details) {
        if (details.classes() == null || details.classes().isEmpty()) {
            return List.of();
        }
        return details.classes().values().stream()
                .filter(c -> c.name() != null && !c.name().isBlank())
                .map(c -> new OrisEventProjection.Category(c.id(), c.name()))
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
