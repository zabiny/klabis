package club.klabis.shared.config.hateoas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

@Component
class CollectionAffordancesClearingProcessor implements RepresentationModelProcessor<CollectionModel<EntityModel<Object>>> {
    private static final Logger LOG = LoggerFactory.getLogger(CollectionAffordancesClearingProcessor.class);

    @Override
    public CollectionModel<EntityModel<Object>> process(CollectionModel<EntityModel<Object>> model) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Removing selfAffordances from items of {}",
                    ResolvableType.forClass(model.getClass()).getType().getTypeName());
        }

        model.getContent().forEach(typed -> {
            typed.mapLink(IanaLinkRelations.SELF, Link::withoutAffordances);
        });

        return model;
    }
}
