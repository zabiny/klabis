package club.klabis.events.infrastructure.restapi;

import club.klabis.events.infrastructure.restapi.dto.EventResponse;
import club.klabis.shared.application.OrisIntegrationComponent;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.affordBetter;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@OrisIntegrationComponent
class EventResponseCollectionModelPostprocessor implements RepresentationModelProcessor<CollectionModel<EntityModel<EventResponse>>> {

    @Override
    public CollectionModel<EntityModel<EventResponse>> process(CollectionModel<EntityModel<EventResponse>> model) {
        model.mapLink(IanaLinkRelations.SELF,
                selfLink -> selfLink.andAffordances(affordBetter(methodOn(OrisEventsController.class).synchronizeAllEventsWithOris())));

        return model;
    }
}
