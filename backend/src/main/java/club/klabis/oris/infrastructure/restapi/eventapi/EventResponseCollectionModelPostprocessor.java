package club.klabis.oris.infrastructure.restapi.eventapi;

import club.klabis.events.infrastructure.restapi.dto.EventResponse;
import club.klabis.oris.infrastructure.restapi.OrisApi;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.KlabisSecurityService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

@Component
class EventResponseCollectionModelPostprocessor implements RepresentationModelProcessor<CollectionModel<EntityModel<EventResponse>>> {

    private final KlabisSecurityService klabisSecurityService;

    public EventResponseCollectionModelPostprocessor(KlabisSecurityService klabisSecurityService) {
        this.klabisSecurityService = klabisSecurityService;
    }

    @Override
    public CollectionModel<EntityModel<EventResponse>> process(CollectionModel<EntityModel<EventResponse>> model) {
        if (klabisSecurityService.hasGrant(ApplicationGrant.SYSTEM_ADMIN)) {
            model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(OrisApi.class)
                    .synchronizeEventsFromOris(null)).withRel("synchronizeAll"));
        }

        return model;
    }
}
