package club.klabis.events.infrastructure.restapi;

import club.klabis.events.domain.Event;
import club.klabis.events.infrastructure.restapi.dto.EventListResponse;
import club.klabis.members.MemberId;
import club.klabis.shared.config.hateoas.AbstractRepresentationModelMapper;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import java.util.Collection;
import java.util.Optional;

@Mapper(config = DomainToDtoMapperConfiguration.class, componentModel = "spring")
abstract class EventModelMapper extends AbstractRepresentationModelMapper<Event, EventListResponse> {

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

    ;

    @Override
    public Collection<Link> createItemLinks(Event event) {
        Collection<Link> result = super.createItemLinks(event);

        final MemberId memberId = new MemberId(1);

        if (event.isMemberRegistered(memberId) && event.areRegistrationsOpen()) {
            result.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EventRegistrationsController.class)
                            .submitRegistrationForm(event.getId().value(), memberId.value(), null))
                    .withRel("updateRegistration"));

            result.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EventRegistrationsController.class)
                    .cancelEventRegistration(event.getId().value(), memberId.value())).withRel("cancelRegistration"));
        } else {
            result.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EventRegistrationsController.class)
                            .submitRegistrationForm(event.getId().value(), memberId.value(), null))
                    .withRel("createRegistration"));
        }

        return result;

    }

}
