package club.klabis.events.infrastructure.restapi;

import club.klabis.events.application.EventsRepository;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.Registration;
import club.klabis.events.domain.forms.EventRegistrationForm;
import club.klabis.members.MemberId;
import club.klabis.shared.config.hateoas.ModelAssembler;
import club.klabis.shared.config.hateoas.ModelPreparator;
import club.klabis.shared.config.hateoas.RootModel;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.shared.config.restapi.ApiController;
import club.klabis.shared.config.restapi.KlabisPrincipal;
import club.klabis.shared.config.restapi.ResponseViews;
import club.klabis.shared.config.security.HasMemberGrant;
import club.klabis.shared.config.security.KlabisSecurityService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URL;
import java.time.LocalDate;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@ApiController(path = "/member/{memberId}/registration", openApiTagName = "Events")
class RegistrationsController {

    private final ModelAssembler<EventAndMember, RegistrationDto> modelAssembler;
    private final EventsRepository eventsRepository;

    RegistrationsController(ModelAssembler<EventAndMember, RegistrationDto> modelAssembler, EventsRepository eventsRepository) {
        this.modelAssembler = modelAssembler;
        this.eventsRepository = eventsRepository;
    }

    enum EventType {TRAINING, COMPETITION}

    record Event(
            LocalDate date,
            String name,
            String location,
            String organizer,
            EventType eventType,
            LocalDate registrationsDeadline,
            MemberId coordinator,
            @JsonView(ResponseViews.Detailed.class)
            URL website
    ) {

    }

    record RegistrationDto(@JsonProperty(access = JsonProperty.Access.READ_ONLY) @JsonUnwrapped Event event,
                           @JsonUnwrapped
                           @JsonView(ResponseViews.Detailed.class) EventRegistrationForm formData) {

    }

    @HasMemberGrant(memberId = "#memberId")
    @JsonView(ResponseViews.Summary.class)
    @GetMapping
    public PagedModel<EntityModel<RegistrationDto>> getRegistrations(@PathVariable MemberId memberId, Pageable pageable) {
        EventsRepository.EventsQuery eventsQuery = EventsRepository.EventsQuery.query();

        return modelAssembler.toPagedResponse(eventsRepository.findEvents(eventsQuery, pageable)
                .map(e -> new EventAndMember(e, memberId)));
    }


    @GetMapping("/{eventId}")
    @HasMemberGrant(memberId = "#memberId")
    @JsonView(ResponseViews.Detailed.class)
    public EntityModel<RegistrationDto> getRegistration(@PathVariable MemberId memberId, @PathVariable club.klabis.events.domain.Event.Id eventId) {
        return eventsRepository.findById(eventId)
                .filter(club.klabis.events.domain.Event::areRegistrationsOpen)
                .map(e -> new EventAndMember(e, memberId))
                .map(modelAssembler::toEntityResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Registration for member %s and event %s doesn't exist".formatted(memberId, eventId)));
    }

    @PutMapping("/{eventId}")
    @Transactional
    public ResponseEntity<Void> saveRegistration(@PathVariable MemberId memberId, @PathVariable club.klabis.events.domain.Event.Id eventId, @RequestBody EventRegistrationForm form) {
        club.klabis.events.domain.Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Event %s doesn't exist".formatted(eventId)));


        if (event.isMemberRegistered(memberId)) {
            event.registerMember(memberId, form);

            URI registrationDetailUri = linkTo(methodOn(getClass()).getRegistration(memberId, eventId)).toUri();
            return ResponseEntity.created(registrationDetailUri).build();
        } else {
            event.changeRegistration(memberId, form);
            return ResponseEntity.noContent().build();
        }
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> saveRegistration(@PathVariable MemberId memberId, @PathVariable club.klabis.events.domain.Event.Id eventId) {
        club.klabis.events.domain.Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Event %s doesn't exist".formatted(eventId)));

        event.cancelMemberRegistration(memberId);
        return ResponseEntity.noContent().build();
    }
}

@Component
class RegistrationsRootResourceProcessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    private final KlabisSecurityService klabisRequestContext;

    RegistrationsRootResourceProcessor(KlabisSecurityService klabisRequestContext) {
        this.klabisRequestContext = klabisRequestContext;
    }

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisRequestContext.getPrincipal().map(KlabisPrincipal::memberId).ifPresent(memberId -> {
            model.add(linkTo(methodOn(RegistrationsController.class).getRegistrations(memberId, null)).withRel(
                    "myRegistrations"));
        });

        return model;
    }
}

record EventAndMember(Event event, MemberId memberId) {
}

@Mapper(config = DomainToDtoMapperConfiguration.class)
abstract class RegistrationDtoAssembler implements ModelPreparator<EventAndMember, RegistrationsController.RegistrationDto> {

    @Override
    public RegistrationsController.RegistrationDto toResponseDto(EventAndMember eventAndMember) {
        return new RegistrationsController.RegistrationDto(
                toDto(eventAndMember.event()),
                toForm(eventAndMember.event(), eventAndMember.memberId())
        );
    }

    @Mapping(target = "registrationsDeadline", source = "registrationDeadline")
    @Mapping(target = "eventType", ignore = true)
    @Mapping(target = "website", source = "website")
    @Mapping(target = "coordinator", source = "coordinator")
    abstract RegistrationsController.Event toDto(Event event);

    abstract EventRegistrationForm toForm(Registration registration);

    EventRegistrationForm emptyRegistrationForm() {
        return new EventRegistrationForm(null, null);
    }

    EventRegistrationForm toForm(Event event, @Context MemberId memberId) {
        if (memberId == null) {
            return emptyRegistrationForm();
        }
        return event.getRegistrationForMember(memberId).map(this::toForm).orElse(null);
    }

}