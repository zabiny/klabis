package com.klabis.events.infrastructure.restapi;

import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.events.application.EventManagementService;
import com.klabis.events.domain.Event;
import com.klabis.events.EventId;
import com.klabis.events.domain.EventStatus;
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
import com.klabis.common.mvc.MvcComponent;
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
public class EventController {

    private final EventManagementService eventManagementService;
    private final PagedResourcesAssembler<EventSummaryDto> pagedResourcesAssembler;

    public EventController(
            EventManagementService eventManagementService,
            PagedResourcesAssembler<EventSummaryDto> pagedResourcesAssembler) {
        this.eventManagementService = eventManagementService;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
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
            @Valid @RequestBody Event.CreateCommand command) {

        Event created = eventManagementService.createEvent(command);

        return ResponseEntity
                .created(klabisLinkTo(methodOn(EventController.class).getEvent(created.getId().value())).toUri())
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
            @Parameter(description = "Event update data") @Valid @RequestBody Event.UpdateCommand command) {

        eventManagementService.updateEvent(new EventId(id), command);
        return ResponseEntity.noContent().build();
    }

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

        EventDto eventDto = EventDtoMapper.toDto(eventManagementService.getEvent(new EventId(id)));

        EntityModel<EventDto> entityModel = EntityModel.of(eventDto);
        addLinksForEvent(entityModel, eventDto);

        return ResponseEntity.ok(entityModel);
    }

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
                ? eventManagementService.listEventsByStatus(status, pageable).map(EventDtoMapper::toSummaryDto)
                : eventManagementService.listEvents(pageable).map(EventDtoMapper::toSummaryDto);

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

    @PostMapping("/{id}/finish")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Finish an event",
            description = "Transitions event from ACTIVE to FINISHED status."
    )
    @ApiResponse(responseCode = "204", description = "Event finished successfully")
    public ResponseEntity<Void> finishEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID id) {

        eventManagementService.finishEvent(new EventId(id));
        return ResponseEntity.noContent().build();
    }

    private void addLinksForEvent(EntityModel<?> entityModel, EventDto eventDto) {
        UUID eventId = eventDto.id();
        EventStatus status = eventDto.status();

        Link selfLink = klabisLinkTo(methodOn(EventController.class).getEvent(eventId)).withSelfRel();

        switch (status) {
            case DRAFT:
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).updateEvent(eventId, null)));
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).publishEvent(eventId)));
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).cancelEvent(eventId)));
                break;

            case ACTIVE:
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).updateEvent(eventId, null)));
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).cancelEvent(eventId)));
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).finishEvent(eventId)));
                break;

            case FINISHED:
            case CANCELLED:
                break;
        }

        entityModel.add(selfLink);
        entityModel.add(klabisLinkTo(methodOn(EventController.class).listEvents(null, null)).withRel("collection"));
        entityModel.add(Link.of("/api/events/" + eventId + "/registrations").withRel("registrations"));
    }

}

@MvcComponent
class EventsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        model.add(klabisLinkTo(methodOn(EventController.class).listEvents(null, Pageable.unpaged())).withRel("events"));
        return model;
    }
}
