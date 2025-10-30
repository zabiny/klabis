package club.klabis.shared.config.hateoas;

import club.klabis.events.domain.Event;
import club.klabis.finance.infrastructure.restapi.FinanceAccountsController;
import club.klabis.members.MemberId;
import club.klabis.members.domain.Member;
import club.klabis.shared.config.restapi.ApiController;
import club.klabis.shared.config.restapi.KlabisUserAuthentication;
import club.klabis.users.domain.ApplicationUser;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.hal.HalModelBuilder;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

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
    public RepresentationModel<?> rootNavigation(@AuthenticationPrincipal ApplicationUser user) {
        RepresentationModel<?> result = HalModelBuilder.emptyHalModel()
                .link(entityLinks.linkToCollectionResource(Member.class)
                        .withRel(linkRelationProvider.getCollectionResourceRelFor(Member.class)))
                .link(entityLinks.linkToCollectionResource(Event.class)
                        .withRel(linkRelationProvider.getCollectionResourceRelFor(Event.class)))
                .build();

        // TODO: fix getAccount parameter - create Principal object which will hold UserId, MemberId and grants
        result.addIf(user != null,
                () -> linkTo(methodOn(FinanceAccountsController.class).getAccount(new MemberId(user.getId()
                        .value()))).withRel("finance"));

        return result;
    }

    private Optional<KlabisUserAuthentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(KlabisUserAuthentication.class::isInstance)
                .map(KlabisUserAuthentication.class::cast);
    }

}
