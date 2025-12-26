package club.klabis.events.infrastructure.restapi;

import club.klabis.events.domain.Event;
import club.klabis.events.infrastructure.restapi.dto.EventResponse;
import club.klabis.shared.application.OrisIntegrationComponent;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.affordBetter;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@OrisIntegrationComponent
class EventResponseModelPostprocessor implements RepresentationModelProcessor<EntityModel<EventResponse>> {

    @Override
    public EntityModel<EventResponse> process(EntityModel<EventResponse> model) {
        Event event = model.getContent().source();

        if (event.getOrisId().isPresent()) {
            model.mapLink(IanaLinkRelations.SELF,
                    selfLink -> selfLink.andAffordances(affordBetter(methodOn(OrisEventsController.class).synchronizeEventWithOris(
                            event.getId()))));
        }

        return model;
    }
}
