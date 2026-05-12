package com.klabis.events.eventtype.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.events.EventTypeId;
import com.klabis.events.eventtype.application.EventTypeManagementPort;
import com.klabis.events.eventtype.domain.EventType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/api/event-types", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "EventTypes", description = "Event type catalog management API")
@PrimaryAdapter
@ExposesResourceFor(EventType.class)
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.EVENTS_SCOPE})
public class EventTypeController {

    private final EventTypeManagementPort eventTypeManagementService;

    EventTypeController(EventTypeManagementPort eventTypeManagementService) {
        this.eventTypeManagementService = eventTypeManagementService;
    }

    @GetMapping
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(summary = "List all event types", description = "Returns all event types sorted by sort_order. Requires EVENTS:MANAGE authority.")
    @ApiResponse(responseCode = "200", description = "List of event types")
    public ResponseEntity<CollectionModel<EntityModel<EventTypeDto>>> listEventTypes() {
        List<EventType> eventTypes = eventTypeManagementService.listAllSorted();

        List<EntityModel<EventTypeDto>> items = eventTypes.stream()
                .map(eventType -> {
                    EventTypeDto dto = EventTypeDtoMapper.toDto(eventType);
                    EntityModel<EventTypeDto> model = EntityModel.of(dto);
                    klabisLinkTo(methodOn(EventTypeController.class).getEventType(eventType.getId().value()))
                            .ifPresent(link -> model.add(link.withSelfRel()
                                    .andAffordances(klabisAfford(methodOn(EventTypeController.class).updateEventType(eventType.getId().value(), null)))
                                    .andAffordances(klabisAfford(methodOn(EventTypeController.class).deleteEventType(eventType.getId().value())))));
                    return model;
                })
                .toList();

        CollectionModel<EntityModel<EventTypeDto>> collection = CollectionModel.of(items);
        klabisLinkTo(methodOn(EventTypeController.class).listEventTypes()).ifPresent(link ->
                collection.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(EventTypeController.class).createEventType(null)))));

        return ResponseEntity.ok(collection);
    }

    @GetMapping("/{id}")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(summary = "Get event type by ID", description = "Returns a single event type. Requires EVENTS:MANAGE authority.")
    @ApiResponse(responseCode = "200", description = "Event type found")
    public ResponseEntity<EntityModel<EventTypeDto>> getEventType(
            @Parameter(description = "Event type UUID") @PathVariable UUID id) {

        EventType eventType = eventTypeManagementService.getEventType(new EventTypeId(id));
        EventTypeDto dto = EventTypeDtoMapper.toDto(eventType);

        EntityModel<EventTypeDto> model = EntityModel.of(dto);
        klabisLinkTo(methodOn(EventTypeController.class).getEventType(id)).ifPresent(link ->
                model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(EventTypeController.class).updateEventType(id, null)))
                        .andAffordances(klabisAfford(methodOn(EventTypeController.class).deleteEventType(id)))));
        klabisLinkTo(methodOn(EventTypeController.class).listEventTypes())
                .ifPresent(link -> model.add(link.withRel("collection")));

        return ResponseEntity.ok(model);
    }

    @PostMapping(consumes = "application/json")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(summary = "Create an event type", description = "Creates a new event type. Requires EVENTS:MANAGE authority.")
    @ApiResponse(responseCode = "201", description = "Event type created")
    ResponseEntity<Void> createEventType(
            @Parameter(description = "Event type creation data")
            @Valid @RequestBody EventType.CreateEventType command) {

        EventType created = eventTypeManagementService.createEventType(command);
        return ResponseEntity
                .created(linkTo(methodOn(EventTypeController.class).getEventType(created.getId().value())).toUri())
                .build();
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(summary = "Update an event type", description = "Updates an existing event type. Requires EVENTS:MANAGE authority.")
    @ApiResponse(responseCode = "204", description = "Event type updated")
    ResponseEntity<Void> updateEventType(
            @Parameter(description = "Event type UUID") @PathVariable UUID id,
            @Parameter(description = "Event type update data") @Valid @RequestBody EventType.UpdateEventType command) {

        eventTypeManagementService.updateEventType(new EventTypeId(id), command);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(summary = "Delete an event type", description = "Deletes an event type not in use. Requires EVENTS:MANAGE authority.")
    @ApiResponse(responseCode = "204", description = "Event type deleted")
    ResponseEntity<Void> deleteEventType(
            @Parameter(description = "Event type UUID") @PathVariable UUID id) {

        eventTypeManagementService.deleteEventType(new EventTypeId(id));
        return ResponseEntity.noContent().build();
    }
}

@MvcComponent
class EventTypesRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(EventTypeController.class).listEventTypes())
                .ifPresent(link -> model.add(link.withRel("event-types")));
        return model;
    }
}
