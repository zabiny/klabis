package club.klabis.events.infrastructure.restapi;

import club.klabis.events.domain.Event;
import club.klabis.events.infrastructure.restapi.dto.EventListItemApiDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;

@Component
class EventModelAssembler implements RepresentationModelAssembler<Event, EventListItemApiDto> {

    private final PagedResourcesAssembler<Event> pagedResourcesAssembler;

    EventModelAssembler(PagedResourcesAssembler<Event> pagedResourcesAssembler) {
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public EventListItemApiDto toModel(Event event) {
        EventListItemApiDto response = new EventListItemApiDto()
                .id(event.getId().value())
                .date(event.getDate())
                .name(event.getName())
                //.type(EventListItemApiDto.TypeEnum.S)
                .organizer(event.getOrganizer())
                //.coordinator("")
                .registrationDeadline(event.getRegistrationDeadline());

        final int memberId = 1;

        response.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EventRegistrationsController.class)
                .getEventRegistrationForm(response.getId(), memberId)).withRel("register"));

        return response;
    }

    @Override
    public CollectionModel<EventListItemApiDto> toCollectionModel(Iterable<? extends Event> entities) {
        CollectionModel<EventListItemApiDto> result = RepresentationModelAssembler.super.toCollectionModel(entities);

        collectionLinks().forEach(result::add);

        return result;
    }

    private Collection<Link> collectionLinks() {
        final int memberId = 1;

        Collection<Link> result = new ArrayList<>();
        result.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EventRegistrationsController.class)
                .getMemberRegistrations(memberId, Pageable.ofSize(5).first())).withRel("myEvents"));

        return result;
    }

    public PagedModel<EventListItemApiDto> toPagedModel(Page<Event> events) {
        PagedModel<EventListItemApiDto> result = pagedResourcesAssembler.toModel(events, this);

        collectionLinks().forEach(result::add);

        return result;
    }
}
