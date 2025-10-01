package club.klabis.events.infrastructure.restapi;

import club.klabis.events.domain.Event;
import club.klabis.events.domain.Registration;
import club.klabis.events.infrastructure.restapi.dto.EventRegistrationResponse;
import club.klabis.events.infrastructure.restapi.dto.EventResponse;
import club.klabis.events.infrastructure.restapi.dto.EventResponseBuilder;
import club.klabis.members.MemberId;
import club.klabis.oris.infrastructure.restapi.OrisApi;
import club.klabis.shared.config.hateoas.AbstractRepresentationModelMapper;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.KlabisSecurityService;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

@Mapper(config = DomainToDtoMapperConfiguration.class, componentModel = "spring")
abstract class EventModelMapper extends AbstractRepresentationModelMapper<Event, EventResponse> {

    private KlabisSecurityService klabisSecurityService;

    public Integer mapId(Event.Id eventID) {
        return eventID.value();
    }

    @Mapping(target = "type", ignore = true)
    @Mapping(target = "web", source = "website")
    @Mapping(target = "coordinator", ignore = true)
    @Mapping(target = "registrations", source = "eventRegistrations")
    @Mapping(target = "source", source = ".")
    @Override
    public abstract EventResponse toResponse(Event event);

    @Mapping(target = "category", constant = "H12")
    @Mapping(target = "memberId", source = "memberId.value")
    public abstract EventRegistrationResponse toResponse(Registration registration);

    @AfterMapping
    public EventResponse afterModelMap(Event event, @MappingTarget EventResponse eventListResponse) {
        return EventResponseBuilder.builder(eventListResponse)
                .type(EventResponse.TypeEnum.ofEvent(event).orElse(null))
                .coordinator(event.getCoordinator().map(MemberId::value).orElse(null))
                .build();
    }

    @Override
    public void addLinks(EntityModel<EventResponse> resource) {
        final MemberId memberId = new MemberId(1);

        Event event = resource.getContent().source();

        resource.add(entityLinks.linkToItemResource(Event.class, event.getId()).withSelfRel());
        resource.add(entityLinks.linkToCollectionResource(Event.class)
                .withRel(linkRelationProvider.getCollectionResourceRelFor(Event.class)));

        if (event.getOrisId().isPresent() && klabisSecurityService.hasGrant(ApplicationGrant.SYSTEM_ADMIN)) {
            resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(OrisApi.class)
                    .synchronizeEventsFromOris(null)).withRel("synchronize"));
        }

        if (event.areRegistrationsOpen()) {
            if (event.isMemberRegistered(memberId)) {
                resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EventRegistrationsController.class)
                                .submitRegistrationForm(event.getId().value(), memberId.value(), null))
                        .withRel("updateRegistration"));

                resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EventRegistrationsController.class)
                                .cancelEventRegistration(event.getId().value(), memberId.value()))
                        .withRel("cancelRegistration"));
            } else {
                resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EventRegistrationsController.class)
                                .submitRegistrationForm(event.getId().value(), memberId.value(), null))
                        .withRel("createRegistration"));
            }
        }
    }

    @Override
    public void addLinks(CollectionModel<EntityModel<EventResponse>> resources) {
        if (klabisSecurityService.hasGrant(ApplicationGrant.SYSTEM_ADMIN)) {
            resources.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(OrisApi.class)
                    .synchronizeEventsFromOris(null)).withRel("synchronizeAll"));
        }
    }

    @Autowired
    public void setKlabisSecurityService(KlabisSecurityService klabisSecurityService) {
        this.klabisSecurityService = klabisSecurityService;
    }
}
