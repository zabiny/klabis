package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.events.EventTypeId;
import com.klabis.events.application.EventTypeManagementPort;
import com.klabis.events.domain.EventType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "EventTypes", description = "Event type catalog management API")
@PrimaryAdapter
@ExposesResourceFor(EventType.class)
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.EVENTS_SCOPE})
public class EventTypeController implements EventTypesApi {

    private final EventTypeManagementPort eventTypeManagementService;

    EventTypeController(EventTypeManagementPort eventTypeManagementService) {
        this.eventTypeManagementService = eventTypeManagementService;
    }

    @Operation(summary = "List all event types", description = "Returns all event types sorted by sort_order. Requires EVENTS:READ authority.")
    @ApiResponse(responseCode = "200", description = "List of event types")
    @Override
    public ResponseEntity<Collection<EventTypeDto>> listEventTypes() {
        List<EventType> eventTypes = eventTypeManagementService.listAllSorted();

        Collection<EventTypeDto> payload = eventTypes.stream()
                .map(EventTypeDtoMapper::toDto)
                .toList();

        HalResponseContext.setDomainList(eventTypes);
        return ResponseEntity.ok(payload);
    }

    @Operation(summary = "Get event type by ID", description = "Returns a single event type. Requires EVENTS:READ authority.")
    @ApiResponse(responseCode = "200", description = "Event type found")
    @Override
    public ResponseEntity<EventTypeDto> getEventType(
            @Parameter(description = "Event type UUID") @PathVariable UUID id) {

        EventType eventType = eventTypeManagementService.getEventType(new EventTypeId(id));

        HalResponseContext.setDomain(eventType);
        return ResponseEntity.ok(EventTypeDtoMapper.toDto(eventType));
    }

    @Operation(summary = "Create an event type", description = "Creates a new event type. Requires EVENTS:MANAGE authority.")
    @ApiResponse(responseCode = "201", description = "Event type created")
    @Override
    public ResponseEntity<Void> createEventType(
            @Parameter(description = "Event type creation data") @RequestBody CreateEventTypeRequest request) {

        EventType created = eventTypeManagementService.createEventType(EventTypeRequestMapper.toCommand(request));
        return ResponseEntity
                .created(linkTo(methodOn(EventTypeController.class).getEventType(created.getId().value())).toUri())
                .build();
    }

    @Operation(summary = "Update an event type", description = "Updates an existing event type. Requires EVENTS:MANAGE authority.")
    @ApiResponse(responseCode = "204", description = "Event type updated")
    @Override
    public ResponseEntity<Void> updateEventType(
            @Parameter(description = "Event type UUID") @PathVariable UUID id,
            @Parameter(description = "Event type update data") @RequestBody UpdateEventTypeRequest request) {

        eventTypeManagementService.updateEventType(new EventTypeId(id), EventTypeRequestMapper.toCommand(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete an event type", description = "Deletes an event type not in use. Requires EVENTS:MANAGE authority.")
    @ApiResponse(responseCode = "204", description = "Event type deleted")
    @Override
    public ResponseEntity<Void> deleteEventType(
            @Parameter(description = "Event type UUID") @PathVariable UUID id) {

        eventTypeManagementService.deleteEventType(new EventTypeId(id));
        return ResponseEntity.noContent().build();
    }
}

@MvcComponent
class EventTypeDetailsPostprocessor extends ModelWithDomainPostprocessor<EventTypeDto, EventType> {

    private final ObjectProvider<EventTypeManagementPort> portProvider;

    EventTypeDetailsPostprocessor(ObjectProvider<EventTypeManagementPort> portProvider) {
        this.portProvider = portProvider;
    }

    @Override
    public void process(EntityModel<EventTypeDto> dtoModel, EventType eventType) {
        UUID id = eventType.getId().value();
        EventTypeManagementPort port = portProvider.getIfAvailable();
        List<HalFormsInlineOption> disciplineOptions = port != null ? port.listDisciplineOptions() : List.of();
        klabisLinkTo(methodOn(EventTypeController.class).getEventType(id)).ifPresent(link ->
                dtoModel.add(link.withSelfRel()
                        .andAffordances(klabisAffordWithPromptedOptions(
                                methodOn(EventTypeController.class).updateEventType(id, null),
                                Map.of("orisDisciplineIds", disciplineOptions)))
                        .andAffordances(klabisAfford(methodOn(EventTypeController.class).deleteEventType(id)))));
        klabisLinkTo(methodOn(EventTypeController.class).listEventTypes())
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
    }
}

/**
 * Adds the collection-level create affordance. The self link itself is built by
 * {@code HalResponseBodyAdvice} from the current request, so this processor only contributes the
 * affordance — which stays authorization-sensitive via {@code klabisAffordWithPromptedOptions}.
 */
@MvcComponent
class EventTypeListPostprocessor
        implements RepresentationModelProcessor<CollectionModel<EntityModel<EventTypeDto>>> {

    private final ObjectProvider<EventTypeManagementPort> portProvider;

    EventTypeListPostprocessor(ObjectProvider<EventTypeManagementPort> portProvider) {
        this.portProvider = portProvider;
    }

    @Override
    public CollectionModel<EntityModel<EventTypeDto>> process(
            CollectionModel<EntityModel<EventTypeDto>> model) {
        EventTypeManagementPort port = portProvider.getIfAvailable();
        List<HalFormsInlineOption> disciplineOptions = port != null ? port.listDisciplineOptions() : List.of();

        model.mapLink(IanaLinkRelations.SELF, selfLink -> (Link) selfLink
                .andAffordances(klabisAffordWithPromptedOptions(
                        methodOn(EventTypeController.class).createEventType(null),
                        Map.of("orisDisciplineIds", disciplineOptions))));
        return model;
    }
}

@MvcComponent
class EventTypesRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Authority.EVENTS_MANAGE.toString()))) {
            return model;
        }
        klabisLinkTo(methodOn(EventTypeController.class).listEventTypes())
                .ifPresent(link -> model.add(link.withRel("event-types")));
        return model;
    }
}
