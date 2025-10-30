package club.klabis.shared.config.hateoas;

import club.klabis.events.domain.Event;
import club.klabis.finance.infrastructure.restapi.FinanceAccountsController;
import club.klabis.members.domain.Member;
import club.klabis.shared.config.restapi.ApiController;
import club.klabis.shared.config.restapi.KlabisPrincipal;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.hal.HalModelBuilder;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@ApiController(path = "/", securityScopes = "klabis", openApiTagName = "Misc")
class RootController {

    private final EntityLinks entityLinks;
    private final LinkRelationProvider linkRelationProvider;

    RootController(EntityLinks entityLinks, LinkRelationProvider linkRelationProvider) {
        this.entityLinks = entityLinks;
        this.linkRelationProvider = linkRelationProvider;
    }

    @GetMapping
    public RepresentationModel<?> rootNavigation(@AuthenticationPrincipal KlabisPrincipal user) {
        RepresentationModel<?> result = HalModelBuilder.emptyHalModel()
                .link(entityLinks.linkToCollectionResource(Member.class)
                        .withRel(linkRelationProvider.getCollectionResourceRelFor(Member.class)))
                .link(entityLinks.linkToCollectionResource(Event.class)
                        .withRel(linkRelationProvider.getCollectionResourceRelFor(Event.class)))
                .build();

        result.addIf(user != null && user.memberId() != null,
                () -> linkTo(methodOn(FinanceAccountsController.class).getAccount(user.memberId())).withRel("finance"));

        return result;
    }

}
