package club.klabis.oris.infrastructure.restapi.eventapi;

import club.klabis.events.domain.Event;
import club.klabis.events.infrastructure.restapi.dto.EventResponse;
import club.klabis.oris.infrastructure.restapi.OrisApi;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.KlabisSecurityService;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.affordBetter;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
class EventResponseModelPostprocessor implements RepresentationModelProcessor<EntityModel<EventResponse>> {

    private final KlabisSecurityService klabisSecurityService;

    public EventResponseModelPostprocessor(KlabisSecurityService klabisSecurityService) {
        this.klabisSecurityService = klabisSecurityService;
    }

    @Override
    public EntityModel<EventResponse> process(EntityModel<EventResponse> model) {
        Event event = model.getContent().source();

        if (event.getOrisId().isPresent() && klabisSecurityService.hasGrant(ApplicationGrant.SYSTEM_ADMIN)) {
            model.mapLink(IanaLinkRelations.SELF,
                    selfLink -> selfLink.andAffordance(affordBetter(methodOn(OrisApi.class).synchronizeEventWithOris(
                            event.getId()))));
        }

        return model;
    }
}
