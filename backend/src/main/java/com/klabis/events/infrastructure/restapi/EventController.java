package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.security.fieldsecurity.SecuritySpelEvaluator;
import com.klabis.common.ui.HalForms;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.events.EventId;
import com.klabis.events.EventTypeId;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.eventtype.infrastructure.restapi.EventTypeController;
import com.klabis.events.application.EventRegistrationPort;
import com.klabis.events.application.OrisEventImportPort;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventFilter;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventStatus;
import com.klabis.members.ActingUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberAccommodationDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import com.klabis.members.infrastructure.restapi.MemberController;
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
import org.springframework.hateoas.mediatype.hal.HalModelBuilder;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
@RequestMapping(value = "/api/events", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Events", description = "Event management API")
@PrimaryAdapter
@ExposesResourceFor(Event.class)
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.EVENTS_SCOPE})
public class EventController {

    private final EventManagementPort eventManagementService;
    private final EventRegistrationPort eventRegistrationService;
    private final Members members;
    private final PagedResourcesAssembler<Event> pagedResourcesAssembler;
    private final boolean orisIntegrationActive;
    private final EventDetailsPostprocessor eventDetailsPostprocessor;

    public EventController(
            EventManagementPort eventManagementService,
            EventRegistrationPort eventRegistrationService,
            Members members,
            PagedResourcesAssembler<Event> pagedResourcesAssembler,
            java.util.Optional<OrisEventImportPort> orisEventImportPort,
            EventDetailsPostprocessor eventDetailsPostprocessor) {
        this.eventManagementService = eventManagementService;
        this.eventRegistrationService = eventRegistrationService;
        this.members = members;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.orisIntegrationActive = orisEventImportPort.isPresent();
        this.eventDetailsPostprocessor = eventDetailsPostprocessor;
    }

    @PostMapping(consumes = "application/json")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Create a new event",
            description = "Creates a new event in DRAFT status. Returns Location header pointing to the created resource."
    )
    @ApiResponse(responseCode = "201", description = "Event successfully created")
    public ResponseEntity<Void> createEvent(
            @Parameter(description = "Event creation data")
            @Valid @RequestBody CreateEventRequest request) {

        Event.CreateEvent command = new Event.CreateEvent(
                request.name(),
                request.eventDate(),
                request.location(),
                request.organizer(),
                request.websiteUrl(),
                request.eventCoordinatorId(),
                request.eventTypeId(),
                request.toRegistrationDeadlines(),
                request.categories()
        );
        Event created = eventManagementService.createEvent(command);

        return ResponseEntity
                .created(linkTo(methodOn(EventController.class).getEvent(created.getId().value(), null)).toUri())
                .build();
    }

    @PatchMapping(value = "/{id}", consumes = "application/json")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Update an event",
            description = "Updates event information. Only allowed for DRAFT and ACTIVE events. Any subset of fields may be provided; absent fields are left unchanged."
    )
    @ApiResponse(responseCode = "204", description = "Event successfully updated")
    public ResponseEntity<Void> updateEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID id,
            @Parameter(description = "Event update data") @Valid @RequestBody UpdateEventRequest request) {

        EventId eventId = new EventId(id);
        Event existingEvent = eventManagementService.getEvent(eventId, true);
        Event.UpdateEvent command = UpdateEventRequestMapper.toCommand(request, existingEvent);
        eventManagementService.updateEvent(eventId, command);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @HasAuthority(Authority.EVENTS_READ)
    @Operation(
            summary = "Get event by ID",
            description = "Retrieves detailed event information by ID. " +
                          "Returns HATEOAS links based on event status and includes embedded registrations."
    )
    @ApiResponse(responseCode = "200", description = "Event found")
    public ResponseEntity<RepresentationModel<?>> getEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID id,
            @ActingUser CurrentUserData currentUser) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Event event = eventManagementService.getEvent(new EventId(id), EventAffordanceSupport.hasAuthority(auth, Authority.EVENTS_MANAGE));

        EventDto eventDto = EventDtoMapper.toDto(event);

        List<RegistrationSummaryDto> registrationDtos = buildRegistrationDtos(event);

        EntityModel<EventDto> entityModel = entityModelWithDomain(eventDto, event);
        // Direct invocation is required because Spring HATEOAS RepresentationModelProcessor
        // does not propagate recursively to EntityModels embedded via HalModelBuilder.
        // The pipeline fires on the outer HalModel, not on the inner EntityModelWithDomain.
        eventDetailsPostprocessor.process(entityModel, event);

        RepresentationModel<?> model = HalModelBuilder.halModelOf(entityModel)
                .embed(registrationDtos, RegistrationSummaryDto.class)
                .build();

        return ResponseEntity.ok(model);
    }

    private List<RegistrationSummaryDto> buildRegistrationDtos(Event event) {
        List<EventRegistration> registrations = eventRegistrationService.listRegistrations(event.getId());
        return RegistrationDtoMapper.toDtoList(registrations, members, event);
    }

    @GetMapping
    @HasAuthority(Authority.EVENTS_READ)
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
    public ResponseEntity<PagedModel<EntityModel<EventSummaryDto>>> listEvents(
            @Parameter(description = "Filter by event status (optional)")
            @RequestParam(required = false) EventStatus status,
            @Parameter(description = "Fulltext search on event name and location (optional)")
            @RequestParam(required = false) String q,
            @Parameter(description = "Filter by organizer code (optional)")
            @RequestParam(required = false) String organizer,
            @Parameter(description = "Filter by coordinator member UUID (optional)")
            @RequestParam(required = false) UUID coordinator,
            @Parameter(description = "Filter by registration: only 'me' is currently accepted (optional)")
            @RequestParam(required = false) String registeredBy,
            @Parameter(description = "Filter events from this date (inclusive, yyyy-MM-dd, optional)")
            @RequestParam(required = false) LocalDate dateFrom,
            @Parameter(description = "Filter events up to this date (inclusive, yyyy-MM-dd, optional)")
            @RequestParam(required = false) LocalDate dateTo,
            @Parameter(description = "Return events whose nearest future registration deadline falls within [today, today+period] (ISO-8601 duration, e.g. P7D, optional)")
            @RequestParam(required = false) Period deadlineWithin,
            @Parameter(description = "Exclude events where the given member is registered: only 'me' is currently accepted (optional)")
            @RequestParam(required = false) String notRegisteredBy,
            @Parameter(description = "Filter by event type UUID (multi-value: ?eventTypeId=x&eventTypeId=y, optional)")
            @RequestParam(required = false) List<UUID> eventTypeId,
            @Parameter(description = "Pagination parameters: page, size, sort")
            @PageableDefault(size = 10, sort = "eventDate", direction = Sort.Direction.DESC) @ParameterObject Pageable pageable,
            @ActingUser CurrentUserData currentUser) {

        validateSortFields(pageable.getSort());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        EventFilter filter = buildFilter(status, q, organizer, coordinator, registeredBy, dateFrom, dateTo, deadlineWithin, notRegisteredBy, eventTypeId, currentUser);
        if (filter == null) {
            return ResponseEntity.ok(pagedResourcesAssembler.toModel(
                    new PageImpl<>(List.of(), pageable, 0),
                    event -> entityModelWithDomain(EventDtoMapper.toSummaryDto(event), event)
            ));
        }
        Page<Event> page = eventManagementService.listEvents(filter, pageable, EventAffordanceSupport.hasAuthority(auth, Authority.EVENTS_MANAGE));

        PagedModel<EntityModel<EventSummaryDto>> pagedModel = pagedResourcesAssembler.toModel(
                page,
                event -> entityModelWithDomain(EventDtoMapper.toSummaryDto(event), event)
        );

        boolean hasManageAuthority = EventAffordanceSupport.hasAuthority(auth, Authority.EVENTS_MANAGE);

        klabisLinkTo(methodOn(EventController.class).listEvents(status, q, organizer, coordinator, registeredBy, dateFrom, dateTo, deadlineWithin, notRegisteredBy, eventTypeId, pageable, null)).ifPresent(link -> {
            Link selfLink = link.withSelfRel()
                    .andAffordances(klabisAfford(methodOn(EventController.class).createEvent(null)));
            if (orisIntegrationActive) {
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(OrisEventController.class).importEvent(null)));
                if (hasManageAuthority) {
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(OrisEventController.class).syncAllUpcomingFromOris()));
                }
            }
            pagedModel.add(selfLink);
        });

        return ResponseEntity.ok(pagedModel);
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
                    .collect(Collectors.toList());
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

    @PostMapping("/{id}/publish")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Publish an event",
            description = "Transitions event from DRAFT to ACTIVE status."
    )
    @ApiResponse(responseCode = "204", description = "Event published successfully")
    public ResponseEntity<Void> publishEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID id) {

        eventManagementService.publishEvent(new EventId(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Cancel an event",
            description = "Transitions event to CANCELLED status. An optional cancellation reason may be provided."
    )
    @ApiResponse(responseCode = "204", description = "Event cancelled successfully")
    public ResponseEntity<Void> cancelEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID id,
            @Parameter(description = "Optional cancellation details")
            @Valid @RequestBody(required = false) CancelEventRequest request) {

        Event.CancelEvent command = request != null
                ? new Event.CancelEvent(request.cancellationReason())
                : Event.CancelEvent.withoutReason();
        eventManagementService.cancelEvent(new EventId(id), command);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/accommodation-list")
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
    public ResponseEntity<CollectionModel<AccommodationListItemDto>> getAccommodationList(
            @Parameter(description = "Event UUID") @PathVariable UUID eventId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Event event = eventManagementService.getEvent(new EventId(eventId), false);

        if (!EventAffordanceSupport.isCoordinatorOrHasRegistrationsAuthority(auth, event)) {
            throw new AccessDeniedException("Access to accommodation list requires EVENTS:REGISTRATIONS authority or being the event coordinator");
        }

        List<EventRegistration> registrations = event.getRegistrations();
        List<MemberId> memberIds = registrations.stream().map(EventRegistration::memberId).toList();
        Map<MemberId, MemberAccommodationDto> accommodationIndex = members.findAccommodationDataByIds(memberIds);

        List<AccommodationListItemDto> items = registrations.stream()
                .map(registration -> toAccommodationListItem(registration, accommodationIndex))
                .toList();

        CollectionModel<AccommodationListItemDto> collectionModel = CollectionModel.of(
                items,
                linkTo(methodOn(EventController.class).getEvent(eventId, null)).withRel("event")
        );

        return ResponseEntity.ok(collectionModel);
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

    static Link addManagementAffordances(Link selfLink, Event event, boolean orisIntegrationActive) {
        UUID eventId = event.getId().value();

        switch (event.getStatus()) {
            case DRAFT:
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).updateEvent(eventId, null)));
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).publishEvent(eventId)));
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).cancelEvent(eventId, null)));
                if (orisIntegrationActive && event.getOrisId() != null) {
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(OrisEventController.class).syncEventFromOris(eventId)));
                }
                break;

            case ACTIVE:
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).updateEvent(eventId, null)));
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).cancelEvent(eventId, null)));
                if (orisIntegrationActive && event.getOrisId() != null) {
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(OrisEventController.class).syncEventFromOris(eventId)));
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

    static boolean isCoordinatorOrHasRegistrationsAuthority(Authentication auth, Event event) {
        if (hasAuthority(auth, Authority.EVENTS_REGISTRATIONS)) {
            return true;
        }
        MemberId coordinatorId = event.getEventCoordinatorId();
        if (coordinatorId == null) {
            return false;
        }
        return coordinatorId.equals(resolveMemberId(auth));
    }
}

@MvcComponent
class EventDetailsPostprocessor extends ModelWithDomainPostprocessor<EventDto, Event> {

    private final boolean orisIntegrationActive;

    EventDetailsPostprocessor(Optional<OrisEventImportPort> orisEventImportPort) {
        this.orisIntegrationActive = orisEventImportPort.isPresent();
    }

    @Override
    public void process(EntityModel<EventDto> dtoModel, Event event) {
        UUID eventId = event.getId().value();

        MemberId currentMemberId = EventAffordanceSupport.resolveMemberId(
                SecurityContextHolder.getContext().getAuthentication());

        klabisLinkTo(methodOn(EventController.class).getEvent(eventId, null)).ifPresent(selfLinkBuilder -> {
            var selfLink = EventAffordanceSupport.addManagementAffordances(selfLinkBuilder.withSelfRel(), event, orisIntegrationActive);

            if (EventAffordanceSupport.shouldOfferRegistration(event)) {
                boolean isRegistered = currentMemberId != null
                        && event.findRegistration(currentMemberId).isPresent();
                if (isRegistered) {
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventRegistrationController.class).unregisterFromEvent(eventId, null)));
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventRegistrationController.class).editRegistration(eventId, currentMemberId.value(), null)));
                } else {
                    selfLink = selfLink.andAffordances(klabisAffordWithOptions(
                            methodOn(EventRegistrationController.class).registerForEvent(eventId, null, null),
                            Map.of("category", event.getCategories())
                    ));
                    if (currentMemberId != null) {
                        klabisLinkTo(methodOn(EventRegistrationController.class).getRegistration(currentMemberId.value(), eventId, true))
                                .ifPresent(link -> dtoModel.add(link.withRel("newRegistration")));
                    }
                }
            }

            dtoModel.add(selfLink);
        });

        klabisLinkTo(methodOn(EventController.class).listEvents(null, null, null, null, null, null, null, null, null, null, null, null))
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));

        if (event.getStatus() != EventStatus.DRAFT) {
            klabisLinkTo(methodOn(EventRegistrationController.class).listRegistrations(eventId, null))
                    .ifPresent(link -> dtoModel.add(link.withRel("registrations").expand()));
        }

        if (event.getEventCoordinatorId() != null) {
            klabisLinkTo(methodOn(MemberController.class).getMember(event.getEventCoordinatorId().value(), null))
                    .ifPresent(link -> dtoModel.add(link.withRel("coordinator")));
        }

        event.getEventTypeId().ifPresent(eventTypeId ->
                klabisLinkTo(methodOn(EventTypeController.class).getEventType(eventTypeId.value()))
                        .ifPresent(link -> dtoModel.add(link.withRel("event-type"))));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (EventAffordanceSupport.isCoordinatorOrHasRegistrationsAuthority(auth, event)) {
            klabisLinkTo(methodOn(EventController.class).getAccommodationList(eventId))
                    .ifPresent(link -> dtoModel.add(link.withRel("accommodation-list")));
        }
    }
}

@MvcComponent
class EventSummaryPostprocessor extends ModelWithDomainPostprocessor<EventSummaryDto, Event> {

    private final boolean orisIntegrationActive;

    EventSummaryPostprocessor(Optional<OrisEventImportPort> orisEventImportPort) {
        this.orisIntegrationActive = orisEventImportPort.isPresent();
    }

    @Override
    public void process(EntityModel<EventSummaryDto> dtoModel, Event event) {
        UUID eventId = event.getId().value();

        MemberId currentMemberId = EventAffordanceSupport.resolveMemberId(
                SecurityContextHolder.getContext().getAuthentication());

        klabisLinkTo(methodOn(EventController.class).getEvent(eventId, null)).ifPresent(selfLinkBuilder -> {
            var selfLink = EventAffordanceSupport.addManagementAffordances(selfLinkBuilder.withSelfRel(), event, orisIntegrationActive);

            if (EventAffordanceSupport.shouldOfferRegistration(event)) {
                boolean isRegistered = currentMemberId != null
                        && event.findRegistration(currentMemberId).isPresent();
                if (isRegistered) {
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventRegistrationController.class).unregisterFromEvent(eventId, null)));
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventRegistrationController.class).editRegistration(eventId, currentMemberId.value(), null)));
                } else {
                    selfLink = selfLink.andAffordances(klabisAffordWithOptions(
                            methodOn(EventRegistrationController.class).registerForEvent(eventId, null, null),
                            Map.of("category", event.getCategories())
                    ));
                    if (currentMemberId != null) {
                        klabisLinkTo(methodOn(EventRegistrationController.class).getRegistration(currentMemberId.value(), eventId, true))
                                .ifPresent(link -> dtoModel.add(link.withRel("newRegistration")));
                    }
                }
            }

            dtoModel.add(selfLink);
        });

        if (event.getEventCoordinatorId() != null) {
            klabisLinkTo(methodOn(MemberController.class).getMember(event.getEventCoordinatorId().value(), null))
                    .ifPresent(link -> dtoModel.add(link.withRel("coordinator")));
        }

        event.getEventTypeId().ifPresent(eventTypeId ->
                klabisLinkTo(methodOn(EventTypeController.class).getEventType(eventTypeId.value()))
                        .ifPresent(link -> dtoModel.add(link.withRel("event-type"))));
    }
}

@MvcComponent
class EventsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(EventController.class).listEvents(null, null, null, null, null, null, null, null, null, null, Pageable.unpaged(), null))
                .ifPresent(link -> model.add(link.withRel("events")));
        klabisLinkTo(methodOn(CategoryPresetController.class).listPresets())
                .ifPresent(link -> model.add(link.withRel("category-presets")));
        return model;
    }
}
