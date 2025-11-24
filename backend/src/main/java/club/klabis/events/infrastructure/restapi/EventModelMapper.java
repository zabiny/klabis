package club.klabis.events.infrastructure.restapi;

import club.klabis.events.domain.Competition;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.Registration;
import club.klabis.events.infrastructure.restapi.dto.EventRegistrationResponse;
import club.klabis.events.infrastructure.restapi.dto.EventResponse;
import club.klabis.events.infrastructure.restapi.dto.EventResponseBuilder;
import club.klabis.members.MemberId;
import club.klabis.shared.config.hateoas.AbstractRepresentationModelMapper;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.shared.config.restapi.KlabisPrincipal;
import club.klabis.shared.config.security.KlabisSecurityService;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Mapper(config = DomainToDtoMapperConfiguration.class, componentModel = "spring")
abstract class EventModelMapper extends AbstractRepresentationModelMapper<Event, EventResponse> {

    private KlabisSecurityService klabisSecurityService;

    @Mapping(target = "type", ignore = true)
    @Mapping(target = "web", source = "website")
    @Mapping(target = "coordinator", ignore = true)
    @Mapping(target = "registrations", source = "eventRegistrations")
    @Mapping(target = "source", source = ".")
    @Override
    public abstract EventResponse toResponse(Event event);

    public abstract EventRegistrationResponse toResponse(Registration registration);

    public abstract String toResponse(Competition.Category registration);

    @AfterMapping
    public EventResponse afterModelMap(Event event, @MappingTarget EventResponse eventListResponse) {
        return EventResponseBuilder.builder(eventListResponse)
                .type(EventResponse.TypeEnum.ofEvent(event).orElse(null))
                .coordinator(event.getCoordinator().map(MemberId::value).orElse(null))
                .build();
    }

    @Override
    public void addLinks(EntityModel<EventResponse> resource) {
        Event event = resource.getContent().source();

        final KlabisPrincipal klabisPrincipal = klabisSecurityService.getPrincipal().orElseThrow();
        final MemberId memberId = klabisPrincipal.memberId();

        resource.add(entityLinks.linkToItemResource(Event.class, event.getId().value()).withSelfRel());
        resource.add(entityLinks.linkToCollectionResource(Event.class)
                .withRel(linkRelationProvider.getCollectionResourceRelFor(Event.class)));

        if (event.areRegistrationsOpen()) {
            if (event.isMemberRegistered(memberId)) {
                resource.add(linkTo(methodOn(EventRegistrationsController.class)
                        .submitRegistrationForm(event.getId(), memberId, null))
                        .withRel("updateRegistration"));

                resource.add(linkTo(methodOn(EventRegistrationsController.class)
                        .cancelEventRegistration(event.getId(), memberId))
                        .withRel("cancelRegistration"));
            } else {
                resource.add(linkTo(methodOn(EventRegistrationsController.class)
                        .submitRegistrationForm(event.getId(), memberId, null))
                        .withRel("createRegistration"));
            }
        }
    }

    @Override
    public void addLinks(CollectionModel<EntityModel<EventResponse>> resources) {
    }

    @Autowired
    public void setKlabisSecurityService(KlabisSecurityService klabisSecurityService) {
        this.klabisSecurityService = klabisSecurityService;
    }

}
