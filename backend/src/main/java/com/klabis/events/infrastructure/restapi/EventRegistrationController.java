package com.klabis.events.infrastructure.restapi;

import com.klabis.common.users.Authority;
import com.klabis.events.EventId;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventRegistrationPort;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.RegistrationNotFoundException;
import com.klabis.members.ActingMember;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
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
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST controller for Event Registration resources.
 * <p>
 * Provides HATEOAS-compliant endpoints for event registration management.
 * All mutation operations require authentication.
 */
@RestController
@RequestMapping(value = "/api/events/{eventId}/registrations", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Event Registrations", description = "Event registration API for members")
@PrimaryAdapter
@ExposesResourceFor(EventRegistration.class)
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.EVENTS_SCOPE})
class EventRegistrationController {

    private final EventManagementPort eventManagementService;
    private final EventRegistrationPort registrationService;
    private final Members members;
    private final EntityLinks entityLinks;

    public EventRegistrationController(EventManagementPort eventManagementService, EventRegistrationPort registrationService, Members members, EntityLinks entityLinks) {
        this.eventManagementService = eventManagementService;
        this.registrationService = registrationService;
        this.members = members;
        this.entityLinks = entityLinks;
    }

    @PostMapping(consumes = "application/json")
    @Operation(
            summary = "Register for an event",
            description = "Register the authenticated member for an event with SI card number. " +
                          "Only allowed for ACTIVE events. Returns Location header pointing to the registration."
    )
    @ApiResponse(responseCode = "201", description = "Successfully registered for event")
    @ApiResponse(responseCode = "409", description = "User already registered to this event")
    public ResponseEntity<Void> registerForEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID eventId,
            @Parameter(description = "Registration data") @Valid @RequestBody Event.RegisterCommand command,
            @ActingMember MemberId actingMember) {

        registrationService.registerMember(new EventId(eventId), actingMember, command);

        return ResponseEntity.created(
                linkTo(methodOn(EventRegistrationController.class).getOwnRegistration(eventId, null)).toUri()
        ).build();
    }

    @DeleteMapping
    @Operation(
            summary = "Unregister from an event",
            description = """
                    Unregister the authenticated member from an event.
                    Only allowed before the event date.
                    """
    )
    @ApiResponse(responseCode = "204", description = "Successfully unregistered")
    public ResponseEntity<Void> unregisterFromEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID eventId,
            @ActingMember MemberId actingMember) {

        registrationService.unregisterMember(new EventId(eventId), actingMember);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(
            summary = "List event registrations",
            description = """
                    List all registrations for an event.
                    SI card numbers are not included for privacy protection.
                    """
    )
    @ApiResponse(responseCode = "200", description = "List of registrations retrieved successfully")
    public ResponseEntity<CollectionModel<RegistrationDto>> listRegistrations(
            @Parameter(description = "Event UUID") @PathVariable UUID eventId) {

        List<EventRegistration> registrations = registrationService.listRegistrations(new EventId(eventId));
        List<RegistrationDto> dtos = RegistrationDtoMapper.toDtoList(registrations, members);

        CollectionModel<RegistrationDto> collectionModel = CollectionModel.of(
                dtos,
                entityLinks.linkForItemResource(Event.class, eventId).withRel("event")
        );
        klabisLinkTo(methodOn(EventRegistrationController.class).listRegistrations(eventId))
                .ifPresent(link -> collectionModel.add(link.withSelfRel()));

        return ResponseEntity.ok(collectionModel);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get own registration",
            description = "Get the authenticated member's registration details including SI card number."
    )
    @ApiResponse(responseCode = "200", description = "Own registration retrieved successfully")
    public ResponseEntity<EntityModel<OwnRegistrationDto>> getOwnRegistration(
            @Parameter(description = "Event UUID") @PathVariable UUID eventId,
            @ActingMember MemberId actingMember) {

        Event event = eventManagementService.getEvent(new EventId(eventId), true);
        EventRegistration registration = event.findRegistration(actingMember)
                .orElseThrow(() -> new RegistrationNotFoundException(actingMember, new EventId(eventId)));

        EntityModel<OwnRegistrationDto> entityModel = EntityModel.of(toOwnRegistrationDto(registration));
        addLinksForOwnRegistration(entityModel, eventId, event);

        return ResponseEntity.ok(entityModel);
    }

    private void addLinksForOwnRegistration(EntityModel<OwnRegistrationDto> entityModel, UUID eventId, Event event) {
        klabisLinkTo(methodOn(EventRegistrationController.class).getOwnRegistration(eventId, null)).ifPresent(selfLinkBuilder -> {
            var selfLink = selfLinkBuilder.withSelfRel();
            if (event.areRegistrationsOpen()) {
                selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventRegistrationController.class).unregisterFromEvent(eventId, null)));
            }
            entityModel.add(selfLink);
        });
        entityModel.add(entityLinks.linkForItemResource(Event.class, eventId).withRel("event"));
    }

    private OwnRegistrationDto toOwnRegistrationDto(EventRegistration registration) {
        MemberDto member = members.findById(registration.memberId())
                .orElseThrow(() -> new IllegalStateException("Member not found for registration: " + registration.memberId()));
        return new OwnRegistrationDto(member.firstName(), member.lastName(), registration.siCardNumber().value(), registration.category(), registration.registeredAt());
    }

}
