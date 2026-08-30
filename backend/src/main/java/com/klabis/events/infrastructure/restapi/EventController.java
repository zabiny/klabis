package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.security.fieldsecurity.SecuritySpelEvaluator;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.events.EventId;
import com.klabis.events.EventTypeId;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventRegistrationPort;
import com.klabis.events.application.MemberRegistrationSanctionPort;
import com.klabis.events.application.OrisEventImportPort;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventFilter;
import com.klabis.events.domain.EventRegistration;
import com.klabis.members.*;
import com.klabis.members.infrastructure.restapi.MembersApi;
import jakarta.annotation.Nullable;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerMapping;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

import static com.klabis.common.ui.HalFormsSupport.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST controller for Event resources.
 * <p>
 * Provides HATEOAS-compliant endpoints for event management.
 * All mutation operations require EVENTS:MANAGE authority.
 */
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@PrimaryAdapter
@ExposesResourceFor(Event.class)
public class EventController implements EventsApi {

    private final EventManagementPort eventManagementService;
    private final EventRegistrationPort eventRegistrationService;
    private final Members members;
    private final AccommodationListCsvRenderer csvRenderer;
    private final ConversionService conversionService;

    public EventController(
            EventManagementPort eventManagementService,
            EventRegistrationPort eventRegistrationService,
            Members members,
            java.util.Optional<OrisEventImportPort> orisEventImportPort,
            AccommodationListCsvRenderer csvRenderer,
            ConversionService conversionService) {
        this.eventManagementService = eventManagementService;
        this.eventRegistrationService = eventRegistrationService;
        this.members = members;
        this.csvRenderer = csvRenderer;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<Void> createEvent(
            CreateEventRequest request) {

        Event.CreateEvent command = conversionService.convert(request, Event.CreateEvent.class);
        Event created = eventManagementService.createEvent(command);

        return ResponseEntity
                .created(linkTo(methodOn(EventsApi.class).getEvent(created.getId().value(), null)).toUri())
                .build();
    }

    @Override
    public ResponseEntity<Void> updateEvent(
            @PathVariable UUID id,
            UpdateEventRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        EventId eventId = new EventId(id);
        Event existingEvent = eventManagementService.getEvent(eventId, true);

        if (!EventAffordanceSupport.isCoordinatorOrHasManageAuthority(auth, existingEvent)) {
            throw new AccessDeniedException("Access to event update requires EVENTS:MANAGE authority or being the event coordinator");
        }

        Event.UpdateEvent prefilled = Event.UpdateEvent.from(existingEvent);
        Event.UpdateEvent command = UpdateEventRequestMapper.toCommand(request, prefilled);
        eventManagementService.updateEvent(eventId, command);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<EventDto> getEvent(
            @PathVariable UUID id,
            @ActingUser CurrentUserData currentUser) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Event event = eventManagementService.getEvent(new EventId(id), EventAffordanceSupport.hasAuthority(auth, Authority.EVENTS_MANAGE));

        // The registrations are declared here rather than in the postprocessor because building them
        // needs the registration port and Members, which @MvcComponent beans should not inject —
        // they are scanned by every @WebMvcTest, so unrelated slice tests would have to mock them.
        HalResponseContext.setDomain(event);
        HalResponseContext.embed(buildRegistrationDtos(event), RegistrationSummaryDto.class);
        return ResponseEntity.ok(conversionService.convert(event, EventDto.class));
    }

    private List<RegistrationSummaryDto> buildRegistrationDtos(Event event) {
        List<EventRegistration> registrations = eventRegistrationService.listRegistrations(event.getId());
        return RegistrationDtoMapper.toDtoList(registrations, members, event);
    }

    @Override
    public ResponseEntity<Page<EventSummaryDto>> listEvents(
            EventStatus status,
            String q,
            String organizer,
            UUID coordinator,
            String registeredBy,
            LocalDate dateFrom,
            LocalDate dateTo,
            String deadlineWithin,
            String notRegisteredBy,
            List<UUID> eventTypeId,
            @PageableDefault(size = 10, sort = "eventDate", direction = Sort.Direction.DESC) @ParameterObject Pageable pageable,
            @ActingUser CurrentUserData currentUser) {

        validateSortFields(pageable.getSort());

        Period deadlinePeriod = deadlineWithin != null ? Period.parse(deadlineWithin) : null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        EventFilter filter = buildFilter(status, q, organizer, coordinator, registeredBy, dateFrom, dateTo, deadlinePeriod, notRegisteredBy, eventTypeId, currentUser);
        if (filter == null) {
            Page<Event> empty = new PageImpl<>(List.of(), pageable, 0);
            HalResponseContext.setDomainList(List.of());
            return ResponseEntity.ok(empty.map(e -> conversionService.convert(e, EventSummaryDto.class)));
        }
        Page<Event> page = eventManagementService.listEvents(filter, pageable, EventAffordanceSupport.hasAuthority(auth, Authority.EVENTS_MANAGE));

        HalResponseContext.setDomainList(page.getContent());
        return ResponseEntity.ok(page.map(e -> conversionService.convert(e, EventSummaryDto.class)));
    }

    /**
     * Builds an {@link EventFilter} from the query parameters received by {@code listEvents}.
     * Returns {@code null} when the request implies an empty result without querying the
     * repository — specifically when {@code registeredBy=me} or {@code notRegisteredBy=me}
     * is requested but the current user has no member profile (silent no-op per design decision).
     *
     * @throws IllegalArgumentException when {@code registeredBy} or {@code notRegisteredBy}
     *         has an unsupported value (anything other than {@code "me"}), which propagates to HTTP 400.
     */
    @Nullable
    private EventFilter buildFilter(
            EventStatus status,
            String q,
            String organizer,
            UUID coordinator,
            String registeredBy,
            LocalDate dateFrom,
            LocalDate dateTo,
            Period deadlineWithin,
            String notRegisteredBy,
            List<UUID> eventTypeId,
            CurrentUserData currentUser) {

        EventFilter filter = status != null ? EventFilter.byStatus(toDomainStatus(status)) : EventFilter.none();

        if (q != null) {
            filter = filter.withFulltext(q);
        }

        if (organizer != null) {
            filter = filter.withOrganizer(organizer);
        }

        if (coordinator != null) {
            filter = filter.withCoordinator(new MemberId(coordinator));
        }

        if (dateFrom != null || dateTo != null) {
            filter = filter.withDateRange(dateFrom, dateTo);
        }

        if (deadlineWithin != null) {
            filter = filter.withDeadlineWithin(deadlineWithin);
        }

        if (registeredBy != null) {
            if (!"me".equals(registeredBy)) {
                throw new IllegalArgumentException(
                        "Unsupported registeredBy value: '" + registeredBy + "'. Only 'me' is currently accepted.");
            }
            if (!currentUser.isMember()) {
                return null;
            }
            filter = filter.withRegisteredBy(currentUser.memberId());
        }

        if (notRegisteredBy != null) {
            if (!"me".equals(notRegisteredBy)) {
                throw new IllegalArgumentException(
                        "Unsupported notRegisteredBy value: '" + notRegisteredBy + "'. Only 'me' is currently accepted.");
            }
            if (!currentUser.isMember()) {
                return null;
            }
            filter = filter.withNotRegisteredBy(currentUser.memberId());
        }

        if (eventTypeId != null && !eventTypeId.isEmpty()) {
            List<EventTypeId> typeIds = eventTypeId.stream()
                    .map(EventTypeId::new)
                    .toList();
            filter = filter.withEventTypeIds(typeIds);
        }

        return filter;
    }

    private static com.klabis.events.domain.EventStatus toDomainStatus(EventStatus status) {
        return status == null ? null : com.klabis.events.domain.EventStatus.valueOf(status.name());
    }

    private void validateSortFields(Sort sort) {
        final var allowedSortFields = Set.of(
                "id",
                "name",
                "eventDate",
                "location",
                "organizer",
                "status",
                "registrationDeadline"
        );

        for (Sort.Order order : sort) {
            if (!allowedSortFields.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Invalid sort field: " + order.getProperty() +
                        ". Allowed fields: " + allowedSortFields
                );
            }
        }
    }

    @Override
    public ResponseEntity<Void> publishEvent(
            @PathVariable UUID id) {

        eventManagementService.publishEvent(new EventId(id));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> cancelEvent(
            @PathVariable UUID id,
            CancelEventRequest request) {

        Event.CancelEvent command = request != null
                ? new Event.CancelEvent(request.cancellationReason())
                : Event.CancelEvent.withoutReason();
        eventManagementService.cancelEvent(new EventId(id), command);
        return ResponseEntity.noContent().build();
    }

    // Narrows the interface's inherited produces (which lists text/csv too, for documentation
    // purposes — see events.yaml) down to the HAL variant only. Without this, Spring sees two
    // handler methods both willing to serve text/csv (this one via the inherited mapping, plus
    // getAccommodationListAsCsv below) and refuses to dispatch — IllegalStateException: Ambiguous
    // handler methods.
    @GetMapping(value = EventsApi.PATH_GET_ACCOMMODATION_LIST, produces = {MediaTypes.HAL_FORMS_JSON_VALUE, "application/problem+json"})
    @Override
    public ResponseEntity<List<AccommodationListItemDto>> getAccommodationList(
            @PathVariable UUID eventId) {

        Event event = loadAuthorizedEventForAccommodation(eventId);
        List<AccommodationListItemDto> items = assembleAccommodationItems(event);

        HalResponseContext.setDomainList(event.getRegistrations());
        return ResponseEntity.ok(items);
    }

    @GetMapping(value = "/api/events/{eventId}/accommodation-list", produces = "text/csv")
    public ResponseEntity<byte[]> getAccommodationListAsCsv(
            @PathVariable UUID eventId) {

        Event event = loadAuthorizedEventForAccommodation(eventId);
        List<AccommodationListItemDto> items = assembleAccommodationItems(event);
        byte[] csv = csvRenderer.renderToBytes(items);

        String filename = "ubytovani-" + EventNameSlugifier.slugify(event.getName()) + ".csv";

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", "text/csv; charset=UTF-8")
                .body(csv);
    }

    private Event loadAuthorizedEventForAccommodation(UUID eventId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Event event = eventManagementService.getEvent(new EventId(eventId), false);
        if (!EventAffordanceSupport.isCoordinatorOrHasRegistrationsAuthority(auth, event)) {
            throw new AccessDeniedException("Access to accommodation list requires EVENTS:REGISTRATIONS authority or being the event coordinator");
        }
        return event;
    }

    private List<AccommodationListItemDto> assembleAccommodationItems(Event event) {
        List<EventRegistration> registrations = event.getRegistrations();
        List<MemberId> memberIds = registrations.stream().map(EventRegistration::memberId).toList();
        Map<MemberId, MemberAccommodationDto> accommodationIndex = members.findAccommodationDataByIds(memberIds);
        return registrations.stream()
                .map(registration -> toAccommodationListItem(registration, accommodationIndex))
                .toList();
    }

    private AccommodationListItemDto toAccommodationListItem(EventRegistration registration, Map<MemberId, MemberAccommodationDto> accommodationIndex) {
        MemberAccommodationDto accommodationData = accommodationIndex.get(registration.memberId());
        if (accommodationData == null) {
            return AccommodationListItemDtoBuilder.builder().build();
        }
        return AccommodationListItemDtoBuilder.builder()
                .firstName(accommodationData.firstName())
                .lastName(accommodationData.lastName())
                .identityCardNumber(accommodationData.identityCardNumber())
                .identityCardValidityDate(accommodationData.identityCardValidityDate())
                .dateOfBirth(accommodationData.dateOfBirth())
                .addressStreet(accommodationData.addressStreet())
                .addressCity(accommodationData.addressCity())
                .addressPostalCode(accommodationData.addressPostalCode())
                .addressCountry(accommodationData.addressCountry())
                .build();
    }

}

class EventAffordanceSupport {

    static boolean hasAuthority(Authentication auth, Authority authority) {
        return SecuritySpelEvaluator.hasAuthority(auth, authority);
    }

    static Link addManagementAffordances(Link selfLink, Event event, boolean orisIntegrationActive, Authentication auth) {
        UUID eventId = event.getId().value();

        boolean canManage = hasAuthority(auth, Authority.EVENTS_MANAGE);
        boolean canUpdate = isCoordinatorOrHasManageAuthority(auth, event);

        if (!canUpdate) {
            return selfLink;
        }

        switch (event.getStatus()) {
            case DRAFT:
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventsApi.class).updateEvent(eventId, null)));
                if (canManage) {
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventsApi.class).publishEvent(eventId)));
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventsApi.class).cancelEvent(eventId, null)));
                }
                if (orisIntegrationActive && event.getOrisId() != null) {
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(OrisEventsApi.class).syncEventFromOris(eventId)));
                }
                break;

            case ACTIVE:
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventsApi.class).updateEvent(eventId, null)));
                if (canManage) {
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventsApi.class).cancelEvent(eventId, null)));
                }
                if (orisIntegrationActive && event.getOrisId() != null) {
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(OrisEventsApi.class).syncEventFromOris(eventId)));
                }
                break;

            case FINISHED:
            case CANCELLED:
                break;
        }

        return selfLink;
    }

    @Nullable
    static MemberId resolveMemberId(Authentication auth) {
        if (auth instanceof KlabisJwtAuthenticationToken token) {
            return token.getMemberIdUuid()
                    .map(MemberId::new)
                    .orElse(null);
        }
        return null;
    }

    static boolean shouldOfferRegistration(Event event) {
        return event.getStatus() == com.klabis.events.domain.EventStatus.ACTIVE && event.areRegistrationsOpen();
    }

    static boolean isCoordinatorOrHasManageAuthority(Authentication auth, Event event) {
        if (hasAuthority(auth, Authority.EVENTS_MANAGE)) {
            return true;
        }
        MemberId memberId = resolveMemberId(auth);
        return memberId != null && event.isCoordinator(memberId);
    }

    static boolean isCoordinatorOrHasRegistrationsAuthority(Authentication auth, Event event) {
        if (hasAuthority(auth, Authority.EVENTS_REGISTRATIONS)) {
            return true;
        }
        MemberId memberId = resolveMemberId(auth);
        return memberId != null && event.isCoordinator(memberId);
    }

    static List<HalFormsInlineOption> categoryInlineOptions(Event event) {
        return event.getCategories().stream()
                .map(category -> new HalFormsInlineOption(category.id().toString(), category.name()))
                .toList();
    }
}

@MvcComponent
class EventDetailsPostprocessor extends ModelWithDomainPostprocessor<EventDto, Event> {

    private final boolean orisIntegrationActive;
    private final MemberRegistrationSanctionPort sanctionPort;

    EventDetailsPostprocessor(Optional<OrisEventImportPort> orisEventImportPort, MemberRegistrationSanctionPort sanctionPort) {
        this.orisIntegrationActive = orisEventImportPort.isPresent();
        this.sanctionPort = sanctionPort;
    }

    @Override
    public void process(EntityModel<EventDto> dtoModel, Event event) {
        UUID eventId = event.getId().value();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        MemberId currentMemberId = EventAffordanceSupport.resolveMemberId(auth);

        klabisLinkTo(methodOn(EventsApi.class).getEvent(eventId, null)).ifPresent(selfLinkBuilder -> {
            var selfLink = EventAffordanceSupport.addManagementAffordances(selfLinkBuilder.withSelfRel(), event, orisIntegrationActive, auth);

            if (EventAffordanceSupport.shouldOfferRegistration(event)) {
                boolean isRegistered = currentMemberId != null
                        && event.findRegistration(currentMemberId).isPresent();
                if (isRegistered) {
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventRegistrationsApi.class).unregisterFromEvent(eventId, null)));
                    selfLink = selfLink.andAffordances(klabisAffordWithPromptedOptions(
                            methodOn(EventRegistrationsApi.class).editRegistration(eventId, currentMemberId.value(), null),
                            Map.of("categoryId", EventAffordanceSupport.categoryInlineOptions(event))
                    ));
                } else if (currentMemberId == null || !sanctionPort.isMemberBlocked(currentMemberId)) {
                    selfLink = selfLink.andAffordances(klabisAffordWithPromptedOptions(
                            methodOn(EventRegistrationsApi.class).registerForEvent(eventId, null, null),
                            Map.of("categoryId", EventAffordanceSupport.categoryInlineOptions(event))
                    ));
                    if (currentMemberId != null) {
                        klabisLinkTo(methodOn(EventRegistrationsApi.class).getRegistration(currentMemberId.value(), eventId, true))
                                .ifPresent(link -> dtoModel.add(link.withRel("newRegistration")));
                    }
                }
            }

            dtoModel.add(selfLink);
        });

        klabisLinkTo(methodOn(EventsApi.class).listEvents(null, null, null, null, null, null, null, null, null, null, null, null))
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));

        if (event.getStatus() != com.klabis.events.domain.EventStatus.DRAFT) {
            klabisLinkTo(methodOn(EventRegistrationsApi.class).listRegistrations(eventId, null))
                    .ifPresent(link -> dtoModel.add(link.withRel("registrations").expand()));
        }

        event.getCoordinators().forEach(coordinatorId ->
                klabisLinkTo(methodOn(MembersApi.class).getMember(coordinatorId.value(), null))
                        .ifPresent(link -> dtoModel.add(link.withRel("coordinator"))));

        event.getEventTypeId().ifPresent(eventTypeId ->
                klabisLinkTo(methodOn(EventTypesApi.class).getEventType(eventTypeId.value()))
                        .ifPresent(link -> dtoModel.add(link.withRel("event-type"))));

        if (EventAffordanceSupport.isCoordinatorOrHasRegistrationsAuthority(auth, event)) {
            klabisLinkTo(methodOn(EventsApi.class).getAccommodationList(eventId))
                    .ifPresent(link -> dtoModel.add(link.withRel("accommodation-list")));
        }
    }
}

@MvcComponent
class EventSummaryPostprocessor extends ModelWithDomainPostprocessor<EventSummaryDto, Event> {

    private final boolean orisIntegrationActive;
    private final MemberRegistrationSanctionPort sanctionPort;

    EventSummaryPostprocessor(Optional<OrisEventImportPort> orisEventImportPort, MemberRegistrationSanctionPort sanctionPort) {
        this.orisIntegrationActive = orisEventImportPort.isPresent();
        this.sanctionPort = sanctionPort;
    }

    @Override
    public void process(EntityModel<EventSummaryDto> dtoModel, Event event) {
        UUID eventId = event.getId().value();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        MemberId currentMemberId = EventAffordanceSupport.resolveMemberId(auth);

        klabisLinkTo(methodOn(EventsApi.class).getEvent(eventId, null)).ifPresent(selfLinkBuilder -> {
            var selfLink = EventAffordanceSupport.addManagementAffordances(selfLinkBuilder.withSelfRel(), event, orisIntegrationActive, auth);

            if (EventAffordanceSupport.shouldOfferRegistration(event)) {
                boolean isRegistered = currentMemberId != null
                        && event.findRegistration(currentMemberId).isPresent();
                if (isRegistered) {
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventRegistrationsApi.class).unregisterFromEvent(eventId, null)));
                    selfLink = selfLink.andAffordances(klabisAffordWithPromptedOptions(
                            methodOn(EventRegistrationsApi.class).editRegistration(eventId, currentMemberId.value(), null),
                            Map.of("categoryId", EventAffordanceSupport.categoryInlineOptions(event))
                    ));
                } else if (currentMemberId == null || !sanctionPort.isMemberBlocked(currentMemberId)) {
                    selfLink = selfLink.andAffordances(klabisAffordWithPromptedOptions(
                            methodOn(EventRegistrationsApi.class).registerForEvent(eventId, null, null),
                            Map.of("categoryId", EventAffordanceSupport.categoryInlineOptions(event))
                    ));
                    if (currentMemberId != null) {
                        klabisLinkTo(methodOn(EventRegistrationsApi.class).getRegistration(currentMemberId.value(), eventId, true))
                                .ifPresent(link -> dtoModel.add(link.withRel("newRegistration")));
                    }
                }
            }

            dtoModel.add(selfLink);
        });

        event.getCoordinators().forEach(coordinatorId ->
                klabisLinkTo(methodOn(MembersApi.class).getMember(coordinatorId.value(), null))
                        .ifPresent(link -> dtoModel.add(link.withRel("coordinator"))));

        event.getEventTypeId().ifPresent(eventTypeId ->
                klabisLinkTo(methodOn(EventTypesApi.class).getEventType(eventTypeId.value()))
                        .ifPresent(link -> dtoModel.add(link.withRel("event-type"))));
    }
}

/**
 * Adds the collection-level affordances (create, and — when ORIS integration is active and the
 * caller has EVENTS:MANAGE — the three ORIS import/sync operations). The self link itself is built
 * by {@code HalResponseBodyAdvice} from the current request.
 */
@MvcComponent
class EventListPostprocessor implements RepresentationModelProcessor<PagedModel<EntityModel<EventSummaryDto>>> {

    private final boolean orisIntegrationActive;

    EventListPostprocessor(Optional<OrisEventImportPort> orisEventImportPort) {
        this.orisIntegrationActive = orisEventImportPort.isPresent();
    }

    @Override
    public PagedModel<EntityModel<EventSummaryDto>> process(PagedModel<EntityModel<EventSummaryDto>> model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean hasManageAuthority = EventAffordanceSupport.hasAuthority(auth, Authority.EVENTS_MANAGE);

        model.mapLink(IanaLinkRelations.SELF, selfLink -> {
            Link link = (Link) selfLink.andAffordances(klabisAfford(methodOn(EventsApi.class).createEvent(null)));
            if (orisIntegrationActive && hasManageAuthority) {
                link = link.andAffordances(klabisAfford(methodOn(OrisEventsApi.class).importEvent(null)));
                link = link.andAffordances(klabisAfford(methodOn(OrisEventsApi.class).importEventsBatch(null)));
                link = link.andAffordances(klabisAfford(methodOn(OrisEventsApi.class).syncAllUpcomingFromOris()));
            }
            return link;
        });
        return model;
    }
}

/**
 * Contributes the {@code event} relation to the accommodation list. The eventId comes from the URI
 * template rather than from an item, because an event with no registrations yields an empty
 * collection with nothing to recover it from.
 */
@MvcComponent
class AccommodationListPostprocessor
        implements RepresentationModelProcessor<CollectionModel<EntityModel<AccommodationListItemDto>>> {

    @Override
    public CollectionModel<EntityModel<AccommodationListItemDto>> process(
            CollectionModel<EntityModel<AccommodationListItemDto>> model) {
        AccommodationListSupport.currentEventId().ifPresent(eventId ->
                klabisLinkTo(methodOn(EventsApi.class).getEvent(eventId, null))
                        .ifPresent(link -> model.add(link.withRel("event"))));
        return model;
    }
}

/**
 * Gives each accommodation row a {@code self} link pointing at the registration it projects. The
 * eventId comes from the URI template, since {@link EventRegistration} carries no reference back to
 * its event.
 */
@MvcComponent
class AccommodationListItemPostprocessor
        extends ModelWithDomainPostprocessor<AccommodationListItemDto, EventRegistration> {

    @Override
    public void process(EntityModel<AccommodationListItemDto> dtoModel, EventRegistration registration) {
        AccommodationListSupport.currentEventId().ifPresent(eventId ->
                klabisLinkTo(methodOn(EventRegistrationsApi.class)
                        .getRegistration(registration.memberId().value(), eventId, false))
                        .ifPresent(link -> dtoModel.add(link.withSelfRel())));
    }
}

final class AccommodationListSupport {

    private AccommodationListSupport() {
    }

    static Optional<UUID> currentEventId() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return Optional.empty();
        }
        Object variables = attrs.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                RequestAttributes.SCOPE_REQUEST);
        if (!(variables instanceof Map<?, ?> pathVariables)) {
            return Optional.empty();
        }
        Object eventId = pathVariables.get("eventId");
        return eventId != null ? Optional.of(UUID.fromString(eventId.toString())) : Optional.empty();
    }
}

@MvcComponent
class EventsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(EventsApi.class).listEvents(null, null, null, null, null, null, null, null, null, null, Pageable.unpaged(), null))
                .ifPresent(link -> model.add(link.withRel("events")));
        klabisLinkTo(methodOn(CategoryPresetsApi.class).listPresets())
                .ifPresent(link -> model.add(link.withRel("category-presets")));
        return model;
    }
}
