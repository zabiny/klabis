package club.klabis.events.infrastructure.restapi;

import club.klabis.events.domain.Competition;
import club.klabis.events.domain.Event;
import club.klabis.events.infrastructure.restapi.dto.EventResponse;
import club.klabis.events.infrastructure.restapi.dto.EventResponseBuilder;
import club.klabis.members.MemberId;
import club.klabis.shared.config.hateoas.ModelPreparator;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.shared.config.security.KlabisSecurityService;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkRelationProvider;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Mapper(config = DomainToDtoMapperConfiguration.class, componentModel = "spring")
abstract class EventModelMapper implements ModelPreparator<Event, EventResponse> {
    private static final Logger LOG = LoggerFactory.getLogger(EventModelMapper.class);

    private KlabisSecurityService klabisSecurityService;

    private EntityLinks entityLinks;

    private LinkRelationProvider linkRelationProvider;


    @Mapping(target = "type", ignore = true)
    @Mapping(target = "web", source = "website")
    @Mapping(target = "coordinator", ignore = true)
    @Mapping(target = "registrations", source = "eventRegistrations")
    @Mapping(target = "source", ignore = true)
    @Override
    public abstract EventResponse toResponseDto(Event event);

    String map(Competition.Category category) {
        return category.name();
    }

    @AfterMapping
    public EventResponse afterModelMap(Event event, @MappingTarget EventResponse eventListResponse) {
        return EventResponseBuilder.builder(eventListResponse)
                .type(EventResponse.TypeEnum.ofEvent(event).orElse(null))
                .coordinator(event.getCoordinator().map(MemberId::value).orElse(null))
                .source(event)
                .build();
    }

    @Override
    public void addLinks(EntityModel<EventResponse> resource, Event event) {

        resource.add(entityLinks.linkToItemResource(Event.class, event.getId().value()).withSelfRel());
        resource.add(entityLinks.linkToCollectionResource(Event.class)
                .withRel(linkRelationProvider.getCollectionResourceRelFor(Event.class)));

        klabisSecurityService.getAuthenticatedMemberId()
                .ifPresentOrElse(memberId -> {
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
                }, () -> {
                    LOG.warn("No authenticated KLabis member available!");
                });
    }

    @Override
    public void addLinks(CollectionModel<EntityModel<EventResponse>> resources) {
    }

    @Autowired
    public void setKlabisSecurityService(KlabisSecurityService klabisSecurityService) {
        this.klabisSecurityService = klabisSecurityService;
    }

    @Autowired
    public void setEntityLinks(EntityLinks entityLinks) {
        this.entityLinks = entityLinks;
    }

    @Autowired
    public void setLinkRelationProvider(LinkRelationProvider linkRelationProvider) {
        this.linkRelationProvider = linkRelationProvider;
    }
}
