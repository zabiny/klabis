package com.klabis.events.management;

import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.users.authorization.HasAuthority;
import com.klabis.events.Event;
import com.klabis.events.EventStatus;
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
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
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
class EventController {

    private final EventManagementService eventManagementService;
    private final PagedResourcesAssembler<EventSummaryDto> pagedResourcesAssembler;

    public EventController(
            EventManagementService eventManagementService,
            PagedResourcesAssembler<EventSummaryDto> pagedResourcesAssembler) {
        this.eventManagementService = eventManagementService;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    /**
     * Create a new event.
     * <p>
     * POST /api/events
     *
     * @param command create event command
     * @return 201 Created with Location header and event resource
     */
    @PostMapping(consumes = "application/json")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Create a new event",
            description = "Creates a new event in DRAFT status. Event coordinator ID is optional. " +
                          "Returns HATEOAS links for resource navigation."
    )
    @ApiResponse(responseCode = "201", description = "Event successfully created")
    public ResponseEntity<EntityModel<EventDto>> createEvent(
            @Parameter(description = "Event creation data")
            @Valid @RequestBody CreateEventCommand command) {

        UUID eventId = eventManagementService.createEvent(command);
        EventDto eventDto = eventManagementService.getEvent(eventId);

        EntityModel<EventDto> entityModel = EntityModel.of(eventDto);
        addLinksForEvent(entityModel, eventDto);

        return ResponseEntity
                .created(klabisLinkTo(methodOn(EventController.class).getEvent(eventId)).toUri())
                .body(entityModel);
    }

    /**
     * Update an event.
     * <p>
     * PATCH /api/events/{id}
     *
     * @param id      event ID
     * @param command update event command
     * @return 200 OK with updated event resource
     */
    @PatchMapping(value = "/{id}", consumes = "application/json")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Update an event",
            description = """
                    Updates event information. Only allowed for DRAFT and ACTIVE events.
                    Returns HATEOAS links for resource navigation.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Event successfully updated")
    public ResponseEntity<EntityModel<EventDto>> updateEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID id,
            @Parameter(description = "Event update data") @Valid @RequestBody UpdateEventCommand command) {

        eventManagementService.updateEvent(id, command);
        EventDto eventDto = eventManagementService.getEvent(id);

        EntityModel<EventDto> entityModel = EntityModel.of(eventDto);
        addLinksForEvent(entityModel, eventDto);

        return ResponseEntity.ok(entityModel);
    }

    /**
     * Get event by ID.
     * <p>
     * GET /api/events/{id}
     *
     * @param id event ID
     * @return event resource with full details
     */
    @GetMapping("/{id}")
    @HasAuthority(Authority.EVENTS_READ)
    @Operation(
            summary = "Get event by ID",
            description = "Retrieves detailed event information by ID. " +
                          "Returns HATEOAS links based on event status."
    )
    @ApiResponse(responseCode = "200", description = "Event found")
    public ResponseEntity<EntityModel<EventDto>> getEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID id) {

        EventDto eventDto = eventManagementService.getEvent(id);

        EntityModel<EventDto> entityModel = EntityModel.of(eventDto);
        addLinksForEvent(entityModel, eventDto);

        return ResponseEntity.ok(entityModel);
    }

    /**
     * List events with pagination and optional filtering.
     * <p>
     * GET /api/events?page=0&size=10&sort=eventDate,desc&status=ACTIVE
     *
     * @param status   optional status filter
     * @param pageable pagination and sorting parameters
     * @return paginated collection of event summaries
     */
    @GetMapping
    @HasAuthority(Authority.EVENTS_READ)
    @Operation(
            summary = "List events with pagination and filtering",
            description = """
                    Retrieves a paginated list of events.
                    Supports filtering by status and sorting by various fields.
                    Default: page=0, size=10, sort=eventDate,desc.
                    Allowed sort fields: id, name, eventDate, location, organizer, status.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Paginated list of events retrieved successfully")
    public ResponseEntity<PagedModel<EntityModel<EventSummaryDto>>> listEvents(
            @Parameter(description = "Filter by event status (optional)")
            @RequestParam(required = false) EventStatus status,
            @Parameter(description = "Pagination parameters: page, size, sort")
            @PageableDefault(size = 10, sort = "eventDate", direction = Sort.Direction.DESC) @ParameterObject Pageable pageable) {

        validateSortFields(pageable.getSort());

        Page<EventSummaryDto> page = status != null
                ? eventManagementService.listEventsByStatus(status, pageable)
                : eventManagementService.listEvents(pageable);

        PagedModel<EntityModel<EventSummaryDto>> pagedModel = pagedResourcesAssembler.toModel(
                page,
                dto -> {
                    EntityModel<EventSummaryDto> model = EntityModel.of(dto);
                    model.add(klabisLinkTo(methodOn(EventController.class).getEvent(dto.id())).withSelfRel());
                    return model;
                }
        );

        pagedModel.add(klabisLinkTo(methodOn(EventController.class).listEvents(status, pageable)).withSelfRel()
                .andAffordances(klabisAfford(methodOn(EventController.class).createEvent(null)))
        );

        return ResponseEntity.ok(pagedModel);
    }

    /**
     * Validates that all sort fields are in the allowed list.
     *
     * @param sort the sort specification to validate
     * @throws IllegalArgumentException if any sort field is not allowed
     */
    private void validateSortFields(Sort sort) {
        final var allowedSortFields = java.util.Set.of(
                "id",
                "name",
                "eventDate",
                "location",
                "organizer",
                "status"
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

    /**
     * Publish an event (DRAFT to ACTIVE).
     * <p>
     * POST /api/events/{id}/publish
     *
     * @param id event ID
     * @return 200 OK with updated event resource
     */
    @PostMapping("/{id}/publish")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Publish an event",
            description = "Transitions event from DRAFT to ACTIVE status."
    )
    @ApiResponse(responseCode = "200", description = "Event published successfully")
    public ResponseEntity<EntityModel<EventDto>> publishEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID id) {

        eventManagementService.publishEvent(id);
        EventDto eventDto = eventManagementService.getEvent(id);

        EntityModel<EventDto> entityModel = EntityModel.of(eventDto);
        addLinksForEvent(entityModel, eventDto);

        return ResponseEntity.ok(entityModel);
    }

    /**
     * Cancel an event (DRAFT/ACTIVE to CANCELLED).
     * <p>
     * POST /api/events/{id}/cancel
     *
     * @param id event ID
     * @return 200 OK with updated event resource
     */
    @PostMapping("/{id}/cancel")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Cancel an event",
            description = "Transitions event to CANCELLED status."
    )
    @ApiResponse(responseCode = "200", description = "Event cancelled successfully")
    public ResponseEntity<EntityModel<EventDto>> cancelEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID id) {

        eventManagementService.cancelEvent(id);
        EventDto eventDto = eventManagementService.getEvent(id);

        EntityModel<EventDto> entityModel = EntityModel.of(eventDto);
        addLinksForEvent(entityModel, eventDto);

        return ResponseEntity.ok(entityModel);
    }

    /**
     * Finish an event (ACTIVE to FINISHED).
     * <p>
     * POST /api/events/{id}/finish
     *
     * @param id event ID
     * @return 200 OK with updated event resource
     */
    @PostMapping("/{id}/finish")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Finish an event",
            description = "Transitions event from ACTIVE to FINISHED status."
    )
    @ApiResponse(responseCode = "200", description = "Event finished successfully")
    public ResponseEntity<EntityModel<EventDto>> finishEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID id) {

        eventManagementService.finishEvent(id);
        EventDto eventDto = eventManagementService.getEvent(id);

        EntityModel<EventDto> entityModel = EntityModel.of(eventDto);
        addLinksForEvent(entityModel, eventDto);

        return ResponseEntity.ok(entityModel);
    }

    /**
     * Add HATEOAS links to event based on its status.
     *
     * @param entityModel the entity model to add links to
     * @param eventDto    the event DTO
     */
    private void addLinksForEvent(EntityModel<?> entityModel, EventDto eventDto) {
        UUID eventId = eventDto.id();
        EventStatus status = eventDto.status();

        Link selfLink = klabisLinkTo(methodOn(EventController.class).getEvent(eventId)).withSelfRel();

        // Status-specific affordances
        switch (status) {
            case DRAFT:
                // DRAFT: can edit, publish, or cancel
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).updateEvent(eventId, null)));
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).publishEvent(eventId)));
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).cancelEvent(eventId)));
                break;

            case ACTIVE:
                // ACTIVE: can edit, cancel, or finish
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).updateEvent(eventId, null)));
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).cancelEvent(eventId)));
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).finishEvent(eventId)));
                break;

            case FINISHED:
            case CANCELLED:
                // FINISHED/CANCELLED: read-only, no edit/transition links
                break;
        }

        // Self link - always present
        entityModel.add(selfLink);

        // Collection link - always present
        entityModel.add(klabisLinkTo(methodOn(EventController.class).listEvents(null, null)).withRel("collection"));

        // Registrations link - always present (links to event registration endpoint)
        entityModel.add(Link.of("/api/events/" + eventId + "/registrations").withRel("registrations"));
    }

}

@Component
class EventsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        model.add(klabisLinkTo(methodOn(EventController.class).listEvents(null, Pageable.unpaged())).withRel("events"));
        return model;
    }
}