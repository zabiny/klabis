package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.security.fieldsecurity.SecuritySpelEvaluator;
import com.klabis.common.ui.HalForms;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.events.EventId;
import com.klabis.events.EventTypeId;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventRegistrationPort;
import com.klabis.events.application.MemberRegistrationSanctionPort;
import com.klabis.events.application.OrisEventImportPort;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventFilter;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventStatus;
import com.klabis.members.*;
import com.klabis.members.infrastructure.restapi.MembersApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
@Tag(name = "Events", description = "Event management API")
@PrimaryAdapter
@ExposesResourceFor(Event.class)
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.EVENTS_SCOPE})
public class EventController implements EventsApi {

    private final EventManagementPort eventManagementService;
    private final EventRegistrationPort eventRegistrationService;
    private final Members members;
    private final boolean orisIntegrationActive;
    private final AccommodationListCsvRenderer csvRenderer;

    public EventController(
            EventManagementPort eventManagementService,
            EventRegistrationPort eventRegistrationService,
            Members members,
            java.util.Optional<OrisEventImportPort> orisEventImportPort,
            AccommodationListCsvRenderer csvRenderer) {
        this.eventManagementService = eventManagementService;
        this.eventRegistrationService = eventRegistrationService;
        this.members = members;
        this.orisIntegrationActive = orisEventImportPort.isPresent();
        this.csvRenderer = csvRenderer;
    }

    @Operation(
            summary = "Create a new event",
            description = "Creates a new event in DRAFT status. Returns Location header pointing to the created resource."
    )
    @ApiResponse(responseCode = "201", description = "Event successfully created")
    @Override
    public ResponseEntity<Void> createEvent(
            @Parameter(description = "Event creation data")
            CreateEventRequest request) {

        Event.CreateEvent command = new Event.CreateEvent(
                request.name(),
                request.eventDate(),
                request.location(),
                request.organizer(),
                request.websiteUrl(),
                request.coordinators(),
                request.eventTypeId(),
                request.toRegistrationDeadlines(),
                request.toCategories()
        );
        Event created = eventManagementService.createEvent(command);

        return ResponseEntity
                .created(linkTo(methodOn(EventsApi.class).getEvent(created.getId().value(), null)).toUri())
                .build();
    }

    @Operation(
            summary = "Update an event",
            description = "Updates event information. Only allowed for DRAFT and ACTIVE events. Any subset of fields may be provided; absent fields are left unchanged."
    )
    @ApiResponse(responseCode = "204", description = "Event successfully updated")
    @Override
    public ResponseEntity<Void> updateEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID id,
            @Parameter(description = "Event update data") UpdateEventRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        EventId eventId = new EventId(id);
        Event existingEvent = eventManagementService.getEvent(eventId, true);

        if (!EventAffordanceSupport.isCoordinatorOrHasManageAuthority(auth, existingEvent)) {
            throw new AccessDeniedException("Access to event update requires EVENTS:MANAGE authority or being the event coordinator");
        }

        Event.UpdateEvent command = UpdateEventRequestMapper.toCommand(request, existingEvent);
        eventManagementService.updateEvent(eventId, command);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get event by ID",
            description = "Retrieves detailed event information by ID. " +
                          "Returns HATEOAS links based on event status and includes embedded registrations."
    )
    @ApiResponse(responseCode = "200", description = "Event found")
    @Override
    public ResponseEntity<EventDto> getEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID id,
            @ActingUser CurrentUserData currentUser) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Event event = eventManagementService.getEvent(new EventId(id), EventAffordanceSupport.hasAuthority(auth, Authority.EVENTS_MANAGE));

        // The registrations are declared here rather than in the postprocessor because building them
        // needs the registration port and Members, which @MvcComponent beans should not inject —
        // they are scanned by every @WebMvcTest, so unrelated slice tests would have to mock them.
        HalResponseContext.setDomain(event);
        HalResponseContext.embed(buildRegistrationDtos(event), RegistrationSummaryDto.class);
        return ResponseEntity.ok(EventDtoMapper.toDto(event));
    }

    private List<RegistrationSummaryDto> buildRegistrationDtos(Event event) {
        List<EventRegistration> registrations = eventRegistrationService.listRegistrations(event.getId());
        return RegistrationDtoMapper.toDtoList(registrations, members, event);
    }

    @Operation(
            summary = "List events with pagination and filtering",
            description = """
                    Retrieves a paginated list of events.
                    Supports filtering by status and sorting by various fields.
                    Default: page=0, size=10, sort=eventDate,desc.
                    Allowed sort fields: id, name, eventDate, location, organizer, status, registrationDeadline.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Paginated list of events retrieved successfully")
    @Override
    public ResponseEntity<Page<EventSummaryDto>> listEvents(
            @Parameter(description = "Filter by event status (optional)")
            EventStatus status,
            @Parameter(description = "Fulltext search on event name and location (optional)")
            String q,
            @Parameter(description = "Filter by organizer code (optional)")
            String organizer,
            @Parameter(description = "Filter by coordinator member UUID (optional)")
            UUID coordinator,
            @Parameter(description = "Filter by registration: only 'me' is currently accepted (optional)")
            String registeredBy,
            @Parameter(description = "Filter events from this date (inclusive, yyyy-MM-dd, optional)")
            LocalDate dateFrom,
            @Parameter(description = "Filter events up to this date (inclusive, yyyy-MM-dd, optional)")
            LocalDate dateTo,
            @Parameter(description = "Return events whose nearest future registration deadline falls within [today, today+period] (ISO-8601 duration, e.g. P7D, optional)")
            String deadlineWithin,
            @Parameter(description = "Exclude events where the given member is registered: only 'me' is currently accepted (optional)")
            String notRegisteredBy,
            @Parameter(description = "Filter by event type UUID (multi-value: ?eventTypeId=x&eventTypeId=y, optional)")
            List<UUID> eventTypeId,
            @Parameter(description = "Pagination parameters: page, size, sort")
            @PageableDefault(size = 10, sort = "eventDate", direction = Sort.Direction.DESC) @ParameterObject Pageable pageable,
            @ActingUser CurrentUserData currentUser) {

        validateSortFields(pageable.getSort());

        Period deadlinePeriod = deadlineWithin != null ? Period.parse(deadlineWithin) : null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        EventFilter filter = buildFilter(status, q, organizer, coordinator, registeredBy, dateFrom, dateTo, deadlinePeriod, notRegisteredBy, eventTypeId, currentUser);
        if (filter == null) {
            Page<Event> empty = new PageImpl<>(List.of(), pageable, 0);
            HalResponseContext.setDomainList(List.of());
            return ResponseEntity.ok(empty.map(EventDtoMapper::toSummaryDto));
        }
        Page<Event> page = eventManagementService.listEvents(filter, pageable, EventAffordanceSupport.hasAuthority(auth, Authority.EVENTS_MANAGE));

        HalResponseContext.setDomainList(page.getContent());
        return ResponseEntity.ok(page.map(EventDtoMapper::toSummaryDto));
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

        EventFilter filter = status != null ? EventFilter.byStatus(status) : EventFilter.none();

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

    @Operation(
            summary = "Publish an event",
            description = "Transitions event from DRAFT to ACTIVE status."
    )
    @ApiResponse(responseCode = "204", description = "Event published successfully")
    @Override
    public ResponseEntity<Void> publishEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID id) {

        eventManagementService.publishEvent(new EventId(id));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Cancel an event",
            description = "Transitions event to CANCELLED status. An optional cancellation reason may be provided."
    )
    @ApiResponse(responseCode = "204", description = "Event cancelled successfully")
    @Override
    public ResponseEntity<Void> cancelEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID id,
            @Parameter(description = "Optional cancellation details")
            CancelEventRequest request) {

        Event.CancelEvent command = request != null
                ? new Event.CancelEvent(request.cancellationReason())
                : Event.CancelEvent.withoutReason();
        eventManagementService.cancelEvent(new EventId(id), command);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get accommodation list for an event",
            description = """
                    Returns the accommodation list for an event with personal details for each registered member.
                    Accessible only to the event coordinator or users with EVENTS:REGISTRATIONS authority.
                    Fields that are not recorded for a member are returned as null.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Accommodation list retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - must be the event coordinator or have EVENTS:REGISTRATIONS")
    // Narrows the interface's inherited produces (which lists text/csv too, for documentation
    // purposes — see events.yaml) down to the HAL variant only. Without this, Spring sees two
    // handler methods both willing to serve text/csv (this one via the inherited mapping, plus
    // getAccommodationListAsCsv below) and refuses to dispatch — IllegalStateException: Ambiguous
    // handler methods.
    @GetMapping(value = EventsApi.PATH_GET_ACCOMMODATION_LIST, produces = {MediaTypes.HAL_FORMS_JSON_VALUE, "application/problem+json"})
    @Override
    public ResponseEntity<CollectionModel<AccommodationListItemDto>> getAccommodationList(
            @Parameter(description = "Event UUID") @PathVariable UUID eventId) {

        Event event = loadAuthorizedEventForAccommodation(eventId);
        List<AccommodationListItemDto> items = assembleAccommodationItems(event);

        CollectionModel<AccommodationListItemDto> collectionModel = CollectionModel.of(
                items,
                linkTo(methodOn(EventsApi.class).getEvent(eventId, null)).withRel("event")
        );

        return ResponseEntity.ok(collectionModel);
    }

    @GetMapping(value = "/api/events/{eventId}/accommodation-list", produces = "text/csv")
    @Operation(
            summary = "Download accommodation list for an event as CSV",
            description = """
                    Returns the accommodation list as a CSV file (UTF-8 BOM, semicolon delimiter, Czech headers).
                    Accessible only to the event coordinator or users with EVENTS:REGISTRATIONS authority.
                    """
    )
    @ApiResponse(responseCode = "200", description = "CSV file downloaded successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - must be the event coordinator or have EVENTS:REGISTRATIONS")
    public ResponseEntity<byte[]> getAccommodationListAsCsv(
            @Parameter(description = "Event UUID") @PathVariable UUID eventId) {

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
            return new AccommodationListItemDto(null, null, null, null, null, null, null, null, null);
        }
        return new AccommodationListItemDto(
                accommodationData.firstName(),
                accommodationData.lastName(),
                accommodationData.identityCardNumber(),
                accommodationData.identityCardValidityDate(),
                accommodationData.dateOfBirth(),
                accommodationData.addressStreet(),
                accommodationData.addressCity(),
                accommodationData.addressPostalCode(),
                accommodationData.addressCountry()
        );
    }

}

record CancelEventRequest(
        @jakarta.validation.constraints.Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
        @HalForms(formInputType = "textarea")
        String cancellationReason
) {}

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
        return event.getStatus() == EventStatus.ACTIVE && event.areRegistrationsOpen();
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

        if (event.getStatus() != EventStatus.DRAFT) {
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
