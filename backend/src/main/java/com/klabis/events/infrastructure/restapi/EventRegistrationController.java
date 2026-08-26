package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.users.Authority;
import com.klabis.events.EventCategoryId;
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
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisAffordWithPromptedOptions;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@PrimaryAdapter
@ExposesResourceFor(EventRegistration.class)
class EventRegistrationController implements EventRegistrationsApi {

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

    @Override
    public ResponseEntity<Void> registerForEvent(
            @PathVariable UUID eventId,
            RegisterEventRequest request,
            @ActingMember MemberId actingMember) {

        Event.RegisterCommand command = new Event.RegisterCommand(
                request.siCardNumber(),
                request.categoryId() != null ? new EventCategoryId(request.categoryId()) : null);
        registrationService.registerMember(new EventId(eventId), actingMember, command);

        return ResponseEntity.created(
                linkTo(methodOn(EventRegistrationsApi.class).getRegistration(actingMember.value(), eventId, false)).toUri()
        ).build();
    }

    @Override
    public ResponseEntity<Void> unregisterFromEvent(
            @PathVariable UUID eventId,
            @ActingMember MemberId actingMember) {

        registrationService.unregisterMember(new EventId(eventId), actingMember);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> editRegistration(
            @PathVariable UUID eventId,
            @PathVariable UUID memberId,
            EditRegistrationRequest request) {

        Event.EditRegistrationCommand command = new Event.EditRegistrationCommand(
                SiCardNumber.of(request.siCardNumber()),
                request.categoryId() != null ? new EventCategoryId(request.categoryId()) : null
        );
        registrationService.editRegistration(new EventId(eventId), new MemberId(memberId), command);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<RegistrationSummaryDto>> listRegistrations(
            @PathVariable UUID eventId,
            @RequestParam(required = false) String sort) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Event event = eventManagementService.getEvent(new EventId(eventId), false);
        List<EventRegistration> registrations = event.getRegistrations();
        Map<MemberId, MemberDto> memberIndex = members.findByIds(registrations.stream().map(EventRegistration::memberId).toList());

        boolean callerCanSortByRegistrationTime = EventAffordanceSupport.isCoordinatorOrHasRegistrationsAuthority(auth, event);
        List<EventRegistration> sorted = RegistrationSortApplier.sort(registrations, memberIndex, event, sort, callerCanSortByRegistrationTime);

        List<RegistrationSummaryDto> payload = sorted.stream()
                .map(registration -> RegistrationDtoMapper.toDto(registration, memberIndex, members, event))
                .toList();
        List<RegistrationView> domainList = sorted.stream()
                .map(registration -> new RegistrationView(event, registration.memberId()))
                .toList();

        HalResponseContext.setDomainList(domainList);
        return ResponseEntity.ok(payload);
    }

    @Override
    public ResponseEntity<RegistrationDto> getRegistration(
            @PathVariable UUID memberId,
            @PathVariable UUID eventId,
            @RequestParam(required = false, defaultValue = "false") Boolean newRegistration) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        MemberId targetMember = new MemberId(memberId);

        if (Boolean.TRUE.equals(newRegistration)) {
            MemberId principalMemberId = EventAffordanceSupport.resolveMemberId(auth);
            if (principalMemberId == null || !principalMemberId.equals(targetMember)) {
                throw new AccessDeniedException(
                        "Not authorized to request registration defaults for member " + memberId);
            }
            MemberDto memberDto = members.findById(targetMember)
                    .orElseThrow(() -> new IllegalStateException("Member not found: " + targetMember));
            RegistrationDto defaults = new RegistrationDto(
                    null,
                    memberDto.firstName(),
                    memberDto.lastName(),
                    null,
                    memberDto.chipNumber()
            );
            Event event = eventManagementService.getEvent(new EventId(eventId), false);
            HalResponseContext.setDomain(new RegistrationView(event, targetMember));
            return ResponseEntity.ok(defaults);
        }

        Event event = eventManagementService.getEvent(new EventId(eventId), EventAffordanceSupport.hasAuthority(auth, Authority.EVENTS_REGISTRATIONS));
        EventRegistration registration = event.findRegistration(targetMember)
                .orElseThrow(() -> new RegistrationNotFoundException(targetMember, new EventId(eventId)));

        RegistrationDto payload = toRegistrationDto(registration, event);
        HalResponseContext.setDomain(new RegistrationView(event, targetMember));
        return ResponseEntity.ok(payload);
    }

    private RegistrationDto toRegistrationDto(EventRegistration registration, Event event) {
        MemberDto member = members.findById(registration.memberId())
                .orElseThrow(() -> new IllegalStateException("Member not found for registration: " + registration.memberId()));
        return new RegistrationDto(RegistrationDtoMapper.toCategoryDto(registration, event), member.firstName(),
                member.lastName(), registration.registeredAt(), registration.siCardNumber().value());
    }

    /**
     * Pairs the event with the target member for {@link RegistrationDetailsPostprocessor} — the
     * event alone is not enough to build the self/edit/unregister links, which depend on which
     * member the registration (or the "new" defaults) belongs to.
     */
    record RegistrationView(Event event, MemberId memberId) {}

}

@MvcComponent
class RegistrationSummaryPostprocessor
        extends ModelWithDomainPostprocessor<RegistrationSummaryDto, EventRegistrationController.RegistrationView> {

    @Override
    public void process(EntityModel<RegistrationSummaryDto> dtoModel, EventRegistrationController.RegistrationView view) {
        Event event = view.event();
        UUID eventId = event.getId().value();
        UUID rowMemberId = view.memberId().value();

        klabisLinkTo(methodOn(EventRegistrationsApi.class).getRegistration(rowMemberId, eventId, false))
                .ifPresent(selfLinkBuilder -> {
                    if (event.areRegistrationsOpen()) {
                        dtoModel.add(selfLinkBuilder.withSelfRel()
                                .andAffordances(klabisAffordWithPromptedOptions(
                                        methodOn(EventRegistrationsApi.class).editRegistration(eventId, rowMemberId, null),
                                        Map.of("categoryId", EventAffordanceSupport.categoryInlineOptions(event)))));
                    } else {
                        dtoModel.add(selfLinkBuilder.withSelfRel());
                    }
                });
    }
}

/**
 * Adds the collection-level {@code event} link — always present, regardless of whether the event
 * has any registrations. The eventId is read off the current request's resolved
 * {@code @PathVariable} map, since {@code CollectionModel<EntityModel<RegistrationSummaryDto>>}
 * carries no reference back to the event when the list is empty. The self link itself is built by
 * {@code HalResponseBodyAdvice}.
 */
@MvcComponent
class RegistrationListPostprocessor
        implements RepresentationModelProcessor<CollectionModel<EntityModel<RegistrationSummaryDto>>> {

    private final EntityLinks entityLinks;

    RegistrationListPostprocessor(EntityLinks entityLinks) {
        this.entityLinks = entityLinks;
    }

    @Override
    public CollectionModel<EntityModel<RegistrationSummaryDto>> process(
            CollectionModel<EntityModel<RegistrationSummaryDto>> model) {
        currentEventId().ifPresent(eventId ->
                model.add(entityLinks.linkForItemResource(Event.class, eventId).withRel("event")));
        return model;
    }

    private static Optional<UUID> currentEventId() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return Optional.empty();
        }
        Object variables = attrs.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                RequestAttributes.SCOPE_REQUEST);
        if (!(variables instanceof Map<?, ?> pathVariables)) {
            return Optional.empty();
        }
        Object eventId = pathVariables.get("eventId");
        return eventId != null ? Optional.of(UUID.fromString(eventId.toString())) : Optional.empty();
    }
}

@MvcComponent
class RegistrationDetailsPostprocessor
        extends ModelWithDomainPostprocessor<RegistrationDto, EventRegistrationController.RegistrationView> {

    private final EntityLinks entityLinks;

    RegistrationDetailsPostprocessor(EntityLinks entityLinks) {
        this.entityLinks = entityLinks;
    }

    @Override
    public void process(EntityModel<RegistrationDto> dtoModel, EventRegistrationController.RegistrationView view) {
        Event event = view.event();
        MemberId memberId = view.memberId();
        UUID eventId = event.getId().value();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        MemberId actingMember = EventAffordanceSupport.resolveMemberId(auth);

        klabisLinkTo(methodOn(EventRegistrationsApi.class).getRegistration(memberId.value(), eventId, false))
                .ifPresent(selfLinkBuilder -> {
                    var selfLink = selfLinkBuilder.withSelfRel();
                    if (event.areRegistrationsOpen()) {
                        selfLink = selfLink
                                .andAffordances(klabisAffordWithPromptedOptions(
                                        methodOn(EventRegistrationsApi.class).editRegistration(eventId, memberId.value(), null),
                                        Map.of("categoryId", EventAffordanceSupport.categoryInlineOptions(event))));
                        if (memberId.equals(actingMember)) {
                            selfLink = selfLink
                                    .andAffordances(klabisAfford(methodOn(EventRegistrationsApi.class).unregisterFromEvent(eventId, null)));
                        }
                    }
                    dtoModel.add(selfLink);
                });
        dtoModel.add(entityLinks.linkForItemResource(Event.class, eventId).withRel("event"));
    }
}
