package club.klabis.events.infrastructure.restapi;

import club.klabis.events.domain.Event;
import club.klabis.shared.config.hateoas.RootModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

@Component
class EventsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {
    private final EntityLinks entityLinks;
    private final LinkRelationProvider linkRelationProvider;

    public EventsRootPostprocessor(EntityLinks entityLinks, LinkRelationProvider linkRelationProvider) {
        this.entityLinks = entityLinks;
        this.linkRelationProvider = linkRelationProvider;
    }

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        model.add(entityLinks.linkToCollectionResource(Event.class)
                .withRel(linkRelationProvider.getCollectionResourceRelFor(Event.class)));

        return model;
    }

}
