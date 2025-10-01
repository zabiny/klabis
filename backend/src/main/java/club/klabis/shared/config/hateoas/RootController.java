package club.klabis.shared.config.hateoas;

import club.klabis.events.domain.Event;
import club.klabis.members.domain.Member;
import club.klabis.shared.config.restapi.ApiController;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.hal.HalModelBuilder;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.web.bind.annotation.GetMapping;

@ApiController(path = "/", securityScopes = "klabis", openApiTagName = "Misc")
class RootController {

    private final EntityLinks entityLinks;
    private final LinkRelationProvider linkRelationProvider;

    RootController(EntityLinks entityLinks, LinkRelationProvider linkRelationProvider) {
        this.entityLinks = entityLinks;
        this.linkRelationProvider = linkRelationProvider;
    }

    @GetMapping
    public RepresentationModel<?> rootNavigation() {
        return HalModelBuilder.emptyHalModel()
                .link(entityLinks.linkToCollectionResource(Member.class)
                        .withRel(linkRelationProvider.getCollectionResourceRelFor(Member.class)))
                .link(entityLinks.linkToCollectionResource(Event.class)
                        .withRel(linkRelationProvider.getCollectionResourceRelFor(Event.class)))
                .build();
    }

}
