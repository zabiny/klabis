package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.security.fieldsecurity.SecuritySpelEvaluator;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.events.EventId;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventRegistrationPort;
import com.klabis.events.application.OrisEventImportPort;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventFilter;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventStatus;
import com.klabis.members.ActingUser;
import com.klabis.members.CurrentUserData;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
            @Valid @RequestBody Event.CreateEvent command) {

        Event created = eventManagementService.createEvent(command);

        return ResponseEntity
                .created(linkTo(methodOn(EventController.class).getEvent(created.getId().value(), null)).toUri())
                .build();
    }

    @PatchMapping(value = "/{id}", consumes = "application/json")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Update an event",
            description = "Updates event information. Only allowed for DRAFT and ACTIVE events."
    )
    @ApiResponse(responseCode = "204", description = "Event successfully updated")
    public ResponseEntity<Void> updateEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID id,
            @Parameter(description = "Event update data") @Valid @RequestBody Event.UpdateEvent command) {

        eventManagementService.updateEvent(new EventId(id), command);
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

        List<RegistrationSummaryDto> registrationDtos = buildRegistrationDtos(new EventId(id));

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

    private List<RegistrationSummaryDto> buildRegistrationDtos(EventId eventId) {
        List<EventRegistration> registrations = eventRegistrationService.listRegistrations(eventId);
        return RegistrationDtoMapper.toDtoList(registrations, members);
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
            @RequestParam(required = false) java.time.LocalDate dateFrom,
            @Parameter(description = "Filter events up to this date (inclusive, yyyy-MM-dd, optional)")
            @RequestParam(required = false) java.time.LocalDate dateTo,
            @Parameter(description = "Pagination parameters: page, size, sort")
            @PageableDefault(size = 10, sort = "eventDate", direction = Sort.Direction.DESC) @ParameterObject Pageable pageable,
            @ActingUser CurrentUserData currentUser) {

        validateSortFields(pageable.getSort());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        EventFilter filter = buildFilter(status, q, organizer, coordinator, registeredBy, dateFrom, dateTo, currentUser);
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

        klabisLinkTo(methodOn(EventController.class).listEvents(status, q, organizer, coordinator, registeredBy, dateFrom, dateTo, pageable, null)).ifPresent(link -> {
            Link selfLink = link.withSelfRel()
                    .andAffordances(klabisAfford(methodOn(EventController.class).createEvent(null)));
            if (orisIntegrationActive) {
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(OrisEventController.class).importEvent(null)));
            }
            pagedModel.add(selfLink);
        });

        return ResponseEntity.ok(pagedModel);
    }

    /**
     * Builds an {@link EventFilter} from the query parameters received by {@code listEvents}.
     * Returns {@code null} when the request implies an empty result without querying the
     * repository — specifically when {@code registeredBy=me} is requested but the current
     * user has no member profile (silent no-op per design decision).
     *
     * @throws IllegalArgumentException when {@code registeredBy} has an unsupported value
     *         (anything other than {@code "me"}), which propagates to HTTP 400.
     */
    @Nullable
    private EventFilter buildFilter(
            EventStatus status,
            String q,
            String organizer,
            UUID coordinator,
            String registeredBy,
            java.time.LocalDate dateFrom,
            java.time.LocalDate dateTo,
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

        return filter;
    }

    private void validateSortFields(Sort sort) {
        final var allowedSortFields = java.util.Set.of(
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
            description = "Transitions event to CANCELLED status."
    )
    @ApiResponse(responseCode = "204", description = "Event cancelled successfully")
    public ResponseEntity<Void> cancelEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID id) {

        eventManagementService.cancelEvent(new EventId(id));
        return ResponseEntity.noContent().build();
    }

}

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
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).cancelEvent(eventId)));
                if (orisIntegrationActive && event.getOrisId() != null) {
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(OrisEventController.class).syncEventFromOris(eventId)));
                }
                break;

            case ACTIVE:
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).updateEvent(eventId, null)));
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).cancelEvent(eventId)));
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
                }
            }

            dtoModel.add(selfLink);
        });

        klabisLinkTo(methodOn(EventController.class).listEvents(null, null, null, null, null, null, null, null, null))
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));

        if (event.getStatus() != EventStatus.DRAFT) {
            klabisLinkTo(methodOn(EventRegistrationController.class).listRegistrations(eventId))
                    .ifPresent(link -> dtoModel.add(link.withRel("registrations")));
        }

        if (event.getEventCoordinatorId() != null) {
            klabisLinkTo(methodOn(MemberController.class).getMember(event.getEventCoordinatorId().value(), null))
                    .ifPresent(link -> dtoModel.add(link.withRel("coordinator")));
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
                }
            }

            dtoModel.add(selfLink);
        });

        if (event.getEventCoordinatorId() != null) {
            klabisLinkTo(methodOn(MemberController.class).getMember(event.getEventCoordinatorId().value(), null))
                    .ifPresent(link -> dtoModel.add(link.withRel("coordinator")));
        }
    }
}

@MvcComponent
class EventsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(EventController.class).listEvents(null, null, null, null, null, null, null, Pageable.unpaged(), null))
                .ifPresent(link -> model.add(link.withRel("events")));
        klabisLinkTo(methodOn(CategoryPresetController.class).listPresets())
                .ifPresent(link -> model.add(link.withRel("category-presets")));
        return model;
    }
}
