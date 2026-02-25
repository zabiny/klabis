package com.klabis.events.registration;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.events.Event;
import com.klabis.events.EventRegistration;
import com.klabis.members.CurrentUser;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
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

    private final EventRegistrationService registrationService;
    private final EntityLinks entityLinks;

    public EventRegistrationController(EventRegistrationService registrationService, EntityLinks entityLinks) {
        this.registrationService = registrationService;
        this.entityLinks = entityLinks;
    }

    /**
     * Register for an event.
     * <p>
     * POST /api/events/{eventId}/registrations
     *
     * @param eventId event ID
     * @param command registration command
     * @return 201 Created with registration details
     */
    @PostMapping(consumes = "application/json")
    @Operation(
            summary = "Register for an event",
            description = "Register the authenticated member for an event with SI card number. " +
                          "Only allowed for ACTIVE events. Returns HATEOAS links for resource navigation."
    )
    @ApiResponse(responseCode = "201", description = "Successfully registered for event")
    @ApiResponse(responseCode = "409", description = "User already registered to this event")
    public ResponseEntity<EntityModel<OwnRegistrationDto>> registerForEvent(
            @Parameter(description = "Event UUID") @PathVariable UUID eventId,
            @Parameter(description = "Registration data") @Valid @RequestBody RegisterForEventCommand command,
            @CurrentUser UserId currentUserId) {

        // Register member
        registrationService.registerMember(eventId, currentUserId, command);

        // Get registration details
        OwnRegistrationDto registration = registrationService.getOwnRegistration(eventId, currentUserId);

        // Build entity model with HATEOAS links
        EntityModel<OwnRegistrationDto> entityModel = EntityModel.of(registration);
        addLinksForOwnRegistration(entityModel, eventId, currentUserId);

        return ResponseEntity.created(klabisLinkTo(methodOn(EventRegistrationController.class).getOwnRegistration(eventId, currentUserId)).toUri())
                .body(entityModel);
    }

    /**
     * Unregister from an event.
     * <p>
     * DELETE /api/events/{eventId}/registrations
     *
     * @param eventId event ID
     * @return 204 No Content
     */
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
            @CurrentUser UserId currentUserId) {

        // Unregister member (use current date)
        registrationService.unregisterMember(eventId, currentUserId, LocalDate.now());

        return ResponseEntity.noContent().build();
    }

    /**
     * List all registrations for an event.
     * <p>
     * GET /api/events/{eventId}/registrations
     *
     * @param eventId event ID
     * @return list of registrations (without SI card numbers)
     */
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

        // Get registrations
        List<RegistrationDto> registrations = registrationService.listRegistrations(eventId);

        // Build collection model with HATEOAS links
        CollectionModel<RegistrationDto> collectionModel = CollectionModel.of(
                registrations,
                klabisLinkTo(methodOn(EventRegistrationController.class).listRegistrations(eventId))
                        .withSelfRel(),
                entityLinks.linkForItemResource(Event.class, eventId).withRel("event")
        );

        return ResponseEntity.ok(collectionModel);
    }

    /**
     * Get own registration details.
     * <p>
     * GET /api/events/{eventId}/registrations/me
     *
     * @param eventId event ID
     * @return own registration with SI card number
     */
    @GetMapping("/me")
    @Operation(
            summary = "Get own registration",
            description = "Get the authenticated member's registration details including SI card number."
    )
    @ApiResponse(responseCode = "200", description = "Own registration retrieved successfully")
    public ResponseEntity<EntityModel<OwnRegistrationDto>> getOwnRegistration(
            @Parameter(description = "Event UUID") @PathVariable UUID eventId,
            @CurrentUser UserId currentUserId) {

        // Get own registration
        OwnRegistrationDto registration = registrationService.getOwnRegistration(eventId, currentUserId);

        // Build entity model with HATEOAS links
        EntityModel<OwnRegistrationDto> entityModel = EntityModel.of(registration);
        addLinksForOwnRegistration(entityModel, eventId, currentUserId);

        return ResponseEntity.ok(entityModel);
    }

    // ========== HATEOAS Link Builders ==========

    /**
     * Add HATEOAS links for own registration resource.
     *
     * @param entityModel entity model to add links to
     * @param eventId     event ID
     * @param currentUserId current user ID
     */
    private void addLinksForOwnRegistration(EntityModel<OwnRegistrationDto> entityModel, UUID eventId, UserId currentUserId) {
        entityModel.add(klabisLinkTo(methodOn(EventRegistrationController.class).getOwnRegistration(eventId, currentUserId)).withSelfRel()
                .andAffordances(klabisAfford(methodOn(EventRegistrationController.class).unregisterFromEvent(eventId, currentUserId)))
        );
        entityModel.add(entityLinks.linkForItemResource(Event.class, eventId).withRel("event"));
    }


    @ExceptionHandler(DuplicateRegistrationException.class)
    public ErrorResponse handleBusinessRuleViolationException(DuplicateRegistrationException ex) {
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, "").title("Registration Conflict").build();
    }
}
