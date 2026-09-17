package com.klabis.events.infrastructure.orissync;

import com.klabis.events.EventCategory;
import com.klabis.events.EventId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventNotFoundException;
import com.klabis.events.application.OrisEventFields;
import com.klabis.events.application.OrisEventFieldsReader;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRanking;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventSyncFromOrisBuilder;
import com.klabis.events.domain.Money;
import com.klabis.events.domain.RegistrationDeadlines;
import com.klabis.common.OrisIntegrationComponent;
import com.klabis.sync.domain.*;
import org.jmolecules.architecture.hexagonal.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The ORIS event {@link SynchronizationAdapter} (design.md D2, D3): inward-only,
 * reaching {@code events} as a module-internal collaborator rather than through a
 * primary port that existed only to cross a package boundary.
 * <p>
 * Reaches the {@code events} module through {@link EventManagementPort} to read the
 * local side, {@link OrisEventFieldsReader} to read the external side, and
 * {@link EventRepository} together with {@code Event.syncFromOris} to write inward —
 * this class now lives inside {@code events.infrastructure}, so these are
 * module-internal dependencies rather than a cross-module port (design.md D2, D4).
 * <p>
 * Declares no outward write, no create on either side, and no sensitive data: ORIS
 * event data is public, and Klabis has no way to push event changes back to ORIS.
 * A local edit to an ORIS-owned field therefore always surfaces as a conflict rather
 * than being silently overwritten or silently sent onward (design.md D6).
 * <p>
 * {@link #readExternal} resolves the ORIS discipline mapping once and carries it on
 * the returned {@link OrisEventProjection#resolvedEventTypeId()} itself, so
 * {@link #applyToLocal} reads it straight off the projection it was given rather than
 * from separate call-order-dependent state — no second ORIS read (task 8.10), and no
 * risk of one record's resolution leaking onto another's write.
 * <p>
 * Classified as {@link Application}, not as a hexagonal adapter: this class holds two
 * hexagonal roles at once — a driven adapter implementing {@code sync}'s
 * {@link SynchronizationAdapter} secondary port, and a driving adapter calling
 * {@code events}' {@code @PrimaryPort} interfaces above. jMolecules cannot express
 * both on one class, since {@code @PrimaryAdapter} and {@code @SecondaryAdapter} are
 * mutually exclusive in the library's own layer predicates, and a
 * {@code @SecondaryAdapter} may never reach a primary port. {@code Application} is the
 * classification that permits the primary-port access D2 prescribes. The D2 dependency
 * direction is unchanged and still correct.
 */
@OrisIntegrationComponent
@Application
class OrisEventSyncAdapter implements SynchronizationAdapter {

    private static final Logger log = LoggerFactory.getLogger(OrisEventSyncAdapter.class);

    private static final SyncCapabilities CAPABILITIES =
            SyncCapabilities.pullOnly();

    private final EventManagementPort eventManagementPort;
    private final OrisEventFieldsReader orisEventFieldsReader;
    private final EventRepository eventRepository;

    OrisEventSyncAdapter(EventManagementPort eventManagementPort,
                          OrisEventFieldsReader orisEventFieldsReader,
                          EventRepository eventRepository) {
        this.eventManagementPort = eventManagementPort;
        this.orisEventFieldsReader = orisEventFieldsReader;
        this.eventRepository = eventRepository;
    }

    @Override
    public SyncEntityType entityType() {
        return SyncEntityType.EVENT;
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
        return OrisEventProjection.class;
    }

    @Override
    public SyncProjection readLocal(String entityId) {
        Event event = eventManagementPort.getEvent(toEventId(entityId), true);
        return OrisEventProjectionMapper.fromEvent(event);
    }

    @Override
    public SyncProjection readExternal(String externalId) {
        return OrisEventFieldsToProjectionMapper.fromOrisFields(
                orisEventFieldsReader.readOrisFields(toOrisId(externalId)));
    }

    /**
     * Always empty: {@code oris-client} offers no cheap per-event or per-list version
     * signal today. {@code getEventList} returns no version field, and
     * {@code EventDetails.version()} is only available after the full read this token
     * exists to avoid — reading it would not save anything. The engine falls back to a
     * full read on every pass, as it does for any adapter without a token
     * (design.md D3).
     */
    @Override
    public Optional<ExternalVersionToken> externalVersion(String externalId) {
        return Optional.empty();
    }

    @Override
    @Transactional
    public void applyToLocal(String entityId, SyncProjection projection) {
        EventId eventId = toEventId(entityId);
        OrisEventProjection orisProjection = (OrisEventProjection) projection;

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        RegistrationDeadlines deadlines = RegistrationDeadlines.of(
                orisProjection.registrationDeadline1(),
                orisProjection.registrationDeadline2(),
                orisProjection.registrationDeadline3());

        EventRanking ranking = orisProjection.rankingLevelId() != null
                ? EventRanking.of(orisProjection.rankingLevelId(), orisProjection.rankingShortName(), orisProjection.rankingName())
                : null;

        Money baseEntryFee = orisProjection.baseEntryFeeAmount() != null
                ? Money.of(orisProjection.baseEntryFeeAmount(), Currency.getInstance(orisProjection.baseEntryFeeCurrency()))
                : null;

        List<EventCategory> categories = orisProjection.categories().stream()
                .map(category -> EventCategory.createFromOris(category.orisId(), category.name()))
                .toList();

        warnIfSyncRemovesCategoriesWithRegistrations(event, categories);

        event.syncFromOris(EventSyncFromOrisBuilder.builder()
                .name(orisProjection.name())
                .eventDate(orisProjection.eventDate())
                .location(orisProjection.location())
                .organizer(orisProjection.organizer())
                .websiteUrl(orisProjection.websiteUrl() != null ? WebsiteUrl.of(orisProjection.websiteUrl()) : null)
                .registrationDeadlines(deadlines)
                .categories(categories)
                .ranking(ranking)
                .baseEntryFee(baseEntryFee)
                .build());

        event.applyAutoMappedEventType(orisProjection.resolvedEventTypeId());

        eventRepository.save(event);
    }

    /**
     * Logs when an inward ORIS sync would drop categories that still carry
     * registrations. Carried verbatim from the deleted
     * {@code OrisEventFieldsGatewayService.warnIfSyncRemovesCategoriesWithRegistrations}
     * (design.md D2) — it is the only diagnostic on this path, and its loss is
     * invisible until an event silently drops a category that had registrations.
     */
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

    @Override
    public void applyToExternal(String externalId, SyncProjection projection) {
        throw new UnsupportedOperationException(
                "The ORIS event adapter declares no outward write capability");
    }

    private static EventId toEventId(String entityId) {
        return new EventId(UUID.fromString(entityId));
    }

    private static int toOrisId(String externalId) {
        try {
            return Integer.parseInt(externalId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid ORIS externalId: " + externalId, ex);
        }
    }
}
