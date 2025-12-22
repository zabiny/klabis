package club.klabis.members.infrastructure.restapi;

import club.klabis.members.domain.Member;
import club.klabis.shared.config.hateoas.RootModel;
import club.klabis.shared.config.security.KlabisSecurityService;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

@Component
@Order(1)
class MembersRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {
    private final EntityLinks entityLinks;
    private final LinkRelationProvider linkRelationProvider;
    private final KlabisSecurityService securityService;

    public MembersRootPostprocessor(EntityLinks entityLinks, LinkRelationProvider linkRelationProvider, KlabisSecurityService securityService) {
        this.entityLinks = entityLinks;
        this.linkRelationProvider = linkRelationProvider;
        this.securityService = securityService;
    }

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        model.add(entityLinks.linkToCollectionResource(Member.class)
                .withRel(linkRelationProvider.getCollectionResourceRelFor(Member.class)));

        return model;
    }

}
