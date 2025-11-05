package club.klabis.members.infrastructure.restapi;

import club.klabis.members.domain.Member;
import club.klabis.shared.config.hateoas.RootModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

@Component
class MembersRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {
    private final EntityLinks entityLinks;
    private final LinkRelationProvider linkRelationProvider;

    public MembersRootPostprocessor(EntityLinks entityLinks, LinkRelationProvider linkRelationProvider) {
        this.entityLinks = entityLinks;
        this.linkRelationProvider = linkRelationProvider;
    }

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        model.add(entityLinks.linkToCollectionResource(Member.class)
                .withRel(linkRelationProvider.getCollectionResourceRelFor(Member.class)));

        return model;
    }

}
