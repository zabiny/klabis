package club.klabis.events.infrastructure.restapi;

import club.klabis.events.domain.Event;
import club.klabis.events.infrastructure.restapi.dto.EventListResponse;
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
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import java.util.Collection;
import java.util.Optional;

@Mapper(config = DomainToDtoMapperConfiguration.class, componentModel = "spring")
abstract class EventModelMapper extends AbstractRepresentationModelMapper<Event, EventListResponse> {

    private KlabisSecurityService klabisSecurityService;

    public Integer mapId(Event.Id eventID) {
        return eventID.value();
    }

    public String mapCoordinatorName(Optional<MemberId> memberId) {
        return memberId.map(id -> "Coordinator member ID %d".formatted(id.value())).orElse("-");
    }

    @Mapping(target = "type", ignore = true)
    @Mapping(target = "web", ignore = true)
    @Mapping(target = "coordinator", ignore = true)
    @Override
    public abstract EventListResponse mapDataFromDomain(Event event);

    @AfterMapping
    public EventListResponse afterModelMap(Event event, @MappingTarget EventListResponse eventListResponse) {
        eventListResponse.setCoordinator(mapCoordinatorName(event.getCoordinator()));
        return eventListResponse;
    }

    @Override
    public Collection<Link> createCollectionLinks() {
        Collection<Link> result = super.createCollectionLinks();

        if (klabisSecurityService.hasGrant(ApplicationGrant.SYSTEM_ADMIN)) {
            result.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(OrisApi.class)
                    .synchronizeEventsFromOris(null)).withRel("synchronizeAll"));
        }

        return result;
    }

    @Override
    public Collection<Link> createItemLinks(Event event) {
        Collection<Link> result = super.createItemLinks(event);

        final MemberId memberId = new MemberId(1);

        if (event.getOrisId().isPresent() && klabisSecurityService.hasGrant(ApplicationGrant.SYSTEM_ADMIN)) {
            result.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(OrisApi.class)
                    .synchronizeEventsFromOris(null)).withRel("synchronize"));
        }

        if (event.areRegistrationsOpen()) {
            if (event.isMemberRegistered(memberId)) {
                result.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EventRegistrationsController.class)
                                .submitRegistrationForm(event.getId().value(), memberId.value(), null))
                        .withRel("updateRegistration"));

                result.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EventRegistrationsController.class)
                                .cancelEventRegistration(event.getId().value(), memberId.value()))
                        .withRel("cancelRegistration"));
            } else {
                result.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EventRegistrationsController.class)
                                .submitRegistrationForm(event.getId().value(), memberId.value(), null))
                        .withRel("createRegistration"));
            }
        }

        return result;

    }

    @Autowired
    public void setKlabisSecurityService(KlabisSecurityService klabisSecurityService) {
        this.klabisSecurityService = klabisSecurityService;
    }
}
