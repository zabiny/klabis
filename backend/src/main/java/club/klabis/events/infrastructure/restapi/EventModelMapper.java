package club.klabis.events.infrastructure.restapi;

import club.klabis.events.domain.Competition;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.commands.EventManagementCommand;
import club.klabis.events.infrastructure.restapi.dto.EventResponse;
import club.klabis.events.infrastructure.restapi.dto.EventResponseBuilder;
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
import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;
import org.springframework.hateoas.mediatype.hal.forms.ImprovedHalFormsAffordanceModel;
import org.springframework.hateoas.server.EntityLinks;

import java.util.ArrayList;
import java.util.List;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.affordBetter;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Mapper(config = DomainToDtoMapperConfiguration.class, componentModel = "spring")
abstract class EventModelMapper implements ModelPreparator<Event, EventResponse> {
    private static final Logger LOG = LoggerFactory.getLogger(EventModelMapper.class);

    private KlabisSecurityService klabisSecurityService;

    private EntityLinks entityLinks;


    @Mapping(target = "type", ignore = true)
    @Mapping(target = "web", source = "website")
    @Mapping(target = "managementForm", source = ".")
    @Mapping(target = "registrations", source = "eventRegistrations")
    @Mapping(target = "source", ignore = true)
    @Override
    public abstract EventResponse toResponseDto(Event event);

    EventManagementCommand toForm(Event event) {
        return EventManagementCommand.fromEvent(event);
    }

    String map(Competition.Category category) {
        return category.name();
    }

    @AfterMapping
    public EventResponse afterModelMap(Event event, @MappingTarget EventResponse eventListResponse) {
        return EventResponseBuilder.builder(eventListResponse)
                .type(EventResponse.TypeEnum.ofEvent(event).orElse(null))
                .source(event)
                .build();
    }

    @Override
    public void addLinks(EntityModel<EventResponse> resource, Event event) {

        List<Affordance> selfAffordances = new ArrayList<>();

        selfAffordances.addAll(affordBetter(methodOn(EventsController.class).updateEventById(event.getId(), null)));

        klabisSecurityService.getAuthenticatedMemberId()
                .ifPresent(memberId -> {
                    if (event.areRegistrationsOpen()) {
                        if (event.isMemberRegistered(memberId)) {
                            // update registration
                            selfAffordances.addAll(affordBetter(methodOn(EventRegistrationsController.class)
                                    .submitRegistrationForm(event.getId(), memberId, null), a -> addOptions(a, event)));

                            // cancel registration
                            selfAffordances.addAll(affordBetter(methodOn(EventRegistrationsController.class)
                                    .cancelEventRegistration(event.getId(), memberId)));
                        } else {
                            // create registration
                            selfAffordances.addAll(affordBetter(methodOn(EventRegistrationsController.class).submitRegistrationForm(
                                    event.getId(),
                                    memberId,
                                    null), a -> addOptions(a, event)));
                        }
                    }
                });

        resource.add(entityLinks.linkToItemResource(Event.class, event.getId().value())
                .withSelfRel()
                .andAffordances(selfAffordances));
    }

    @Override
    public void addLinks(CollectionModel<EntityModel<EventResponse>> resources) {
        ModelPreparator.super.addLinks(resources);

        resources.mapLink(IanaLinkRelations.SELF,
                link -> link.andAffordances(affordBetter(methodOn(EventsController.class).createEvent(null))));
    }

    private void addOptions(ImprovedHalFormsAffordanceModel affordance, Event event) {
        HalFormsOptions categoryOptions = HalFormsOptions.remote(linkTo(methodOn(EventsController.class).getEventCategories(
                        event.getId())).withRel("categories"))
                .withMaxItems(1L);

        affordance.defineOptions("category", categoryOptions);
    }

    @Autowired
    public void setKlabisSecurityService(KlabisSecurityService klabisSecurityService) {
        this.klabisSecurityService = klabisSecurityService;
    }

    @Autowired
    public void setEntityLinks(EntityLinks entityLinks) {
        this.entityLinks = entityLinks;
    }

}
