package com.klabis.events.infrastructure.restapi;

import com.klabis.common.security.fieldsecurity.SecuritySpelEvaluator;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.events.EventId;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventRegistrationPort;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventFilter;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventStatus;
import com.klabis.members.ActingUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.Members;
import com.klabis.members.infrastructure.restapi.MemberController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.*;
import org.springframework.hateoas.mediatype.hal.HalModelBuilder;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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

    public EventController(
            EventManagementPort eventManagementService,
            EventRegistrationPort eventRegistrationService,
            Members members,
            PagedResourcesAssembler<Event> pagedResourcesAssembler,
            @org.springframework.beans.factory.annotation.Value("${oris-integration.enabled:false}") boolean orisIntegrationActive) {
        this.eventManagementService = eventManagementService;
        this.eventRegistrationService = eventRegistrationService;
        this.members = members;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.orisIntegrationActive = orisIntegrationActive;
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

    @PostMapping(value = "/import", consumes = "application/json")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Import event from ORIS",
            description = "Creates a new event in DRAFT status by importing data from ORIS."
    )
    @ApiResponse(responseCode = "201", description = "Event imported successfully")
    public ResponseEntity<Void> importEvent(
            @Parameter(description = "ORIS import command with orisId")
            @Valid @RequestBody Event.ImportCommand command) {

        Event created = eventManagementService.importEventFromOris(command.orisId());

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

        Event event = eventManagementService.getEvent(new EventId(id), hasEventsManageAuthority());

        EventDto eventDto = EventDtoMapper.toDto(event);

        List<RegistrationDto> registrationDtos = buildRegistrationDtos(new EventId(id));

        EntityModel<EventDto> entityModel = EntityModel.of(eventDto);
        addLinksForEvent(entityModel, event, currentUser);

        RepresentationModel<?> model = HalModelBuilder.halModelOf(entityModel)
                .embed(registrationDtos, RegistrationDto.class)
                .build();

        return ResponseEntity.ok(model);
    }

    private List<RegistrationDto> buildRegistrationDtos(EventId eventId) {
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
            @Parameter(description = "Pagination parameters: page, size, sort")
            @PageableDefault(size = 10, sort = "eventDate", direction = Sort.Direction.DESC) @ParameterObject Pageable pageable,
            @ActingUser CurrentUserData currentUser) {

        validateSortFields(pageable.getSort());

        EventFilter filter = status != null ? EventFilter.byStatus(status) : EventFilter.none();
        Page<Event> page = eventManagementService.listEvents(filter, pageable, hasEventsManageAuthority());

        PagedModel<EntityModel<EventSummaryDto>> pagedModel = pagedResourcesAssembler.toModel(
                page,
                event -> {
                    EntityModel<EventSummaryDto> model = EntityModel.of(EventDtoMapper.toSummaryDto(event));
                    addLinksForListItem(model, event, currentUser);
                    return model;
                }
        );

        klabisLinkTo(methodOn(EventController.class).listEvents(status, pageable, null)).ifPresent(link -> {
            var selfLink = link.withSelfRel()
                    .andAffordances(klabisAfford(methodOn(EventController.class).createEvent(null)));

            if (orisIntegrationActive) {
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).importEvent(null)));
            }

            pagedModel.add(selfLink);
        });

        return ResponseEntity.ok(pagedModel);
    }

    private void addLinksForListItem(EntityModel<EventSummaryDto> model, Event event, CurrentUserData currentUser) {
        UUID eventId = event.getId().value();

        klabisLinkTo(methodOn(EventController.class).getEvent(eventId, null)).ifPresent(selfLinkBuilder -> {
            var selfLink = addManagementAffordances(selfLinkBuilder.withSelfRel(), event);

            if (event.areRegistrationsOpen()) {
                boolean isRegistered = currentUser != null && currentUser.isMember()
                        && event.findRegistration(currentUser.memberId()).isPresent();
                if (isRegistered) {
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventRegistrationController.class).unregisterFromEvent(eventId, null)));
                } else {
                    selfLink = selfLink.andAffordances(klabisAffordWithOptions(
                            methodOn(EventRegistrationController.class).registerForEvent(eventId, null, null),
                            Map.of("category", event.getCategories())
                    ));
                }
            }

            model.add(selfLink);
        });

        if (event.getEventCoordinatorId() != null) {
            klabisLinkTo(methodOn(MemberController.class).getMember(event.getEventCoordinatorId().value(), null))
                    .ifPresent(link -> model.add(link.withRel("coordinator")));
        }
    }

    private Link addManagementAffordances(Link selfLink, Event event) {
        UUID eventId = event.getId().value();

        switch (event.getStatus()) {
            case DRAFT:
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).updateEvent(eventId, null)));
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).publishEvent(eventId)));
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).cancelEvent(eventId)));
                break;

            case ACTIVE:
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).updateEvent(eventId, null)));
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).cancelEvent(eventId)));
                break;

            case FINISHED:
            case CANCELLED:
                break;
        }

        if (orisIntegrationActive && event.getOrisId() != null && (event.getStatus() == EventStatus.DRAFT || event.getStatus() == EventStatus.ACTIVE)) {
            selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).syncEventFromOris(eventId)));
        }

        return selfLink;
    }

    private boolean hasEventsManageAuthority() {
        return SecuritySpelEvaluator.hasAuthority(
                SecurityContextHolder.getContext().getAuthentication(), Authority.EVENTS_MANAGE);
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

    @PostMapping("/{id}/sync-from-oris")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Sync event from ORIS",
            description = "Re-fetches event data from ORIS and overwrites all local fields. Only allowed for DRAFT and ACTIVE events with an orisId."
    )
    @ApiResponse(responseCode = "204", description = "Event synced from ORIS successfully")
    public ResponseEntity<Void> syncEventFromOris(
            @Parameter(description = "Event UUID") @PathVariable UUID id) {

        eventManagementService.syncEventFromOris(new EventId(id));
        return ResponseEntity.noContent().build();
    }

    private void addLinksForEvent(EntityModel<?> entityModel, Event event, CurrentUserData currentUser) {
        UUID eventId = event.getId().value();

        klabisLinkTo(methodOn(EventController.class).getEvent(eventId, null)).ifPresent(selfLinkBuilder -> {
            var selfLink = addManagementAffordances(selfLinkBuilder.withSelfRel(), event);

            if (event.getStatus() == EventStatus.ACTIVE && event.areRegistrationsOpen()) {
                boolean isRegistered = currentUser.isMember() && event.findRegistration(currentUser.memberId()).isPresent();
                if (isRegistered) {
                    selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventRegistrationController.class).unregisterFromEvent(eventId, null)));
                } else {
                    selfLink = selfLink.andAffordances(klabisAffordWithOptions(
                            methodOn(EventRegistrationController.class).registerForEvent(eventId, null, null),
                            Map.of("category", event.getCategories())
                    ));
                }
            }

            entityModel.add(selfLink);
        });

        klabisLinkTo(methodOn(EventController.class).listEvents(null, null, null))
                .ifPresent(link -> entityModel.add(link.withRel("collection")));

        if (event.getStatus() != EventStatus.DRAFT) {
            klabisLinkTo(methodOn(EventRegistrationController.class).listRegistrations(eventId))
                    .ifPresent(link -> entityModel.add(link.withRel("registrations")));
        }

        if (event.getEventCoordinatorId() != null) {
            klabisLinkTo(methodOn(MemberController.class).getMember(event.getEventCoordinatorId().value(), null))
                    .ifPresent(link -> entityModel.add(link.withRel("coordinator")));
        }
    }

}
