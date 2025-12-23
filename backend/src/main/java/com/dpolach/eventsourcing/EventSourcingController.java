package com.dpolach.eventsourcing;

import club.klabis.shared.config.hateoas.HalResourceAssembler;
import club.klabis.shared.config.hateoas.ModelAssembler;
import club.klabis.shared.config.hateoas.ModelPreparator;
import club.klabis.shared.config.hateoas.RootModel;
import club.klabis.shared.config.restapi.ApiController;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.HasGrant;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.linkIfAuthorized;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@ApiController(openApiTagName = "System", path = "eventSourcing")
class EventSourcingController {

    private final EventsRepository eventsRepository;
    private final ModelAssembler<BaseEvent, EventDto> hateoasAssembler;

    public EventSourcingController(EventsRepository eventsRepository, ModelPreparator<BaseEvent, EventDto> modelPreparator, PagedResourcesAssembler<BaseEvent> pagedResourcesAssembler) {
        this.eventsRepository = eventsRepository;
        this.hateoasAssembler = new HalResourceAssembler<>(modelPreparator, pagedResourcesAssembler);
    }

    @HasGrant(ApplicationGrant.SYSTEM_ADMIN)
    @GetMapping
    public PagedModel<EntityModel<EventDto>> getEvents(Pageable pageable) {
        return hateoasAssembler.toPagedResponse(eventsRepository.getEvents(pageable));
    }

}

record EventDto(@JsonUnwrapped BaseEvent event) {

}

@Component
class BaseEventPreparator implements ModelPreparator<BaseEvent, EventDto> {

    @Override
    public EventDto toResponseDto(BaseEvent event) {
        return new EventDto(event);
    }
}

@Component
class EventSourcingRootProcessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        linkIfAuthorized(methodOn(EventSourcingController.class).getEvents(Pageable.ofSize(10)))
                .map(linkBuilder -> linkBuilder.withRel("sourceEvents").withTitle("Application events"))
                .ifPresent(model::add);

        return model;
    }
}
