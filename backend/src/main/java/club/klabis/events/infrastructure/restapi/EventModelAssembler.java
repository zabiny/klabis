package club.klabis.events.infrastructure.restapi;

import club.klabis.events.domain.Event;
import club.klabis.events.infrastructure.restapi.dto.EventResponseItem;
import club.klabis.members.MemberId;
import org.springframework.data.domain.Page;
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
class EventModelAssembler implements RepresentationModelAssembler<Event, EventResponseItem> {

    private final PagedResourcesAssembler<Event> pagedResourcesAssembler;

    EventModelAssembler(PagedResourcesAssembler<Event> pagedResourcesAssembler) {
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public EventResponseItem toModel(Event event) {
        EventResponseItem response = new EventResponseItem()
                .id(event.getId().value())
                .date(event.getDate())
                .name(event.getName())
                //.type(EventListItemApiDto.TypeEnum.S)
                .organizer(event.getOrganizer())
                //.coordinator("")
                .registrationDeadline(event.getRegistrationDeadline());

        final MemberId memberId = new MemberId(1);

        if (event.isMemberRegistered(memberId) && event.areRegistrationsOpen()) {
            response.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EventRegistrationsController.class)
                    .submitRegistrationForm(response.getId(), memberId.value(), null)).withRel("updateRegistration"));

            response.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EventRegistrationsController.class)
                    .cancelEventRegistration(response.getId(), memberId.value())).withRel("cancelRegistration"));
        } else {
            response.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EventRegistrationsController.class)
                    .submitRegistrationForm(response.getId(), memberId.value(), null)).withRel("createRegistration"));
        }

        return response;
    }

    @Override
    public CollectionModel<EventResponseItem> toCollectionModel(Iterable<? extends Event> entities) {
        CollectionModel<EventResponseItem> result = RepresentationModelAssembler.super.toCollectionModel(entities);

        collectionLinks().forEach(result::add);

        return result;
    }

    private Collection<Link> collectionLinks() {
        final MemberId memberId = new MemberId(1);

        Collection<Link> result = new ArrayList<>();
        // define links for collection resources (both CollectionModel and PagedModel)


        return result;
    }

    public PagedModel<EventResponseItem> toPagedModel(Page<Event> events) {
        PagedModel<EventResponseItem> result = pagedResourcesAssembler.toModel(events, this);

        collectionLinks().forEach(result::add);

        return result;
    }
}
