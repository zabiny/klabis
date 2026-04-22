package com.klabis.events.infrastructure.restapi;

import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.security.fieldsecurity.OwnerId;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.security.fieldsecurity.SecuritySpelEvaluator;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.events.EventId;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventRegistrationPort;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.RegistrationNotFoundException;
import com.klabis.events.domain.SiCardNumber;
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
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

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
                linkTo(methodOn(EventRegistrationController.class).getRegistration(actingMember.value(), eventId)).toUri()
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

    @PutMapping(value = "/{memberId}", consumes = "application/json")
    @OwnerVisible
    @Operation(
            summary = "Edit own event registration",
            description = "Update SI card number and/or category for the authenticated member's registration. " +
                          "Only allowed when registrations are open."
    )
    @ApiResponse(responseCode = "204", description = "Registration updated successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - can only edit own registration")
    public ResponseEntity<Void> editRegistration(
            @Parameter(description = "Event UUID") @PathVariable UUID eventId,
            @OwnerId @Parameter(description = "Member UUID") @PathVariable UUID memberId,
            @Valid @RequestBody EditRegistrationRequest request) {

        Event.EditRegistrationCommand command = new Event.EditRegistrationCommand(
                SiCardNumber.of(request.siCardNumber()),
                request.category()
        );
        registrationService.editRegistration(new EventId(eventId), new MemberId(memberId), command);

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
    public ResponseEntity<CollectionModel<EntityModel<RegistrationSummaryDto>>> listRegistrations(
            @Parameter(description = "Event UUID") @PathVariable UUID eventId) {

        Event event = eventManagementService.getEvent(new EventId(eventId), false);
        List<EventRegistration> registrations = event.getRegistrations();
        Map<MemberId, MemberDto> memberIndex = members.findByIds(registrations.stream().map(EventRegistration::memberId).toList());
        MemberId actingMember = resolveActingMember();

        List<EntityModel<RegistrationSummaryDto>> items = buildRegistrationItems(registrations, memberIndex, event, actingMember, eventId);

        CollectionModel<EntityModel<RegistrationSummaryDto>> collectionModel = CollectionModel.of(
                items,
                entityLinks.linkForItemResource(Event.class, eventId).withRel("event")
        );
        klabisLinkTo(methodOn(EventRegistrationController.class).listRegistrations(eventId))
                .ifPresent(link -> collectionModel.add(link.withSelfRel()));

        return ResponseEntity.ok(collectionModel);
    }

    private List<EntityModel<RegistrationSummaryDto>> buildRegistrationItems(
            List<EventRegistration> registrations,
            Map<MemberId, MemberDto> memberIndex,
            Event event,
            @Nullable MemberId actingMember,
            UUID eventId) {

        List<EntityModel<RegistrationSummaryDto>> items = new ArrayList<>();
        for (EventRegistration registration : registrations) {
            RegistrationSummaryDto dto = RegistrationDtoMapper.toDto(registration, memberIndex, members);
            EntityModel<RegistrationSummaryDto> item = EntityModel.of(dto);
            if (actingMember != null && actingMember.equals(registration.memberId())) {
                klabisLinkTo(methodOn(EventRegistrationController.class).getRegistration(actingMember.value(), eventId))
                        .ifPresent(selfLinkBuilder -> {
                            if (event.areRegistrationsOpen()) {
                                item.add(selfLinkBuilder.withSelfRel()
                                        .andAffordances(klabisAfford(methodOn(EventRegistrationController.class)
                                                .editRegistration(eventId, actingMember.value(), null))));
                            } else {
                                item.add(selfLinkBuilder.withSelfRel());
                            }
                        });
            }
            items.add(item);
        }
        return items;
    }

    @Nullable
    private MemberId resolveActingMember() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof KlabisJwtAuthenticationToken token) {
            return token.getMemberIdUuid().map(MemberId::new).orElse(null);
        }
        return null;
    }

    @GetMapping("/{memberId}")
    @OwnerVisible
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Get registration by member ID",
            description = "Get a member's event registration including SI card number. " +
                          "Accessible by the member themselves or a user with EVENTS:MANAGE authority."
    )
    @ApiResponse(responseCode = "200", description = "Registration retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - must be the member or have EVENTS:MANAGE")
    @ApiResponse(responseCode = "404", description = "Member not registered for this event")
    public ResponseEntity<EntityModel<RegistrationDto>> getRegistration(
            @OwnerId @Parameter(description = "Member UUID") @PathVariable UUID memberId,
            @Parameter(description = "Event UUID") @PathVariable UUID eventId) {

        Event event = eventManagementService.getEvent(new EventId(eventId), hasEventsManageAuthority());
        MemberId targetMember = new MemberId(memberId);
        EventRegistration registration = event.findRegistration(targetMember)
                .orElseThrow(() -> new RegistrationNotFoundException(targetMember, new EventId(eventId)));

        EntityModel<RegistrationDto> entityModel = EntityModel.of(toRegistrationDto(registration));
        addLinksForRegistration(entityModel, eventId, event, targetMember);

        return ResponseEntity.ok(entityModel);
    }

    private void addLinksForRegistration(EntityModel<RegistrationDto> entityModel, UUID eventId, Event event, MemberId memberId) {
        klabisLinkTo(methodOn(EventRegistrationController.class).getRegistration(memberId.value(), eventId)).ifPresent(selfLinkBuilder -> {
            var selfLink = selfLinkBuilder.withSelfRel();
            MemberId actingMember = resolveActingMember();
            if (event.areRegistrationsOpen() && memberId.equals(actingMember)) {
                selfLink = selfLink
                        .andAffordances(klabisAfford(methodOn(EventRegistrationController.class).unregisterFromEvent(eventId, null)))
                        .andAffordances(klabisAfford(methodOn(EventRegistrationController.class).editRegistration(eventId, memberId.value(), null)));
            }
            entityModel.add(selfLink);
        });
        entityModel.add(entityLinks.linkForItemResource(Event.class, eventId).withRel("event"));
    }

    private RegistrationDto toRegistrationDto(EventRegistration registration) {
        MemberDto member = members.findById(registration.memberId())
                .orElseThrow(() -> new IllegalStateException("Member not found for registration: " + registration.memberId()));
        return new RegistrationDto(member.firstName(), member.lastName(), registration.siCardNumber().value(), registration.category(), registration.registeredAt());
    }

    private boolean hasEventsManageAuthority() {
        return SecuritySpelEvaluator.hasAuthority(
                SecurityContextHolder.getContext().getAuthentication(), Authority.EVENTS_MANAGE);
    }

}
