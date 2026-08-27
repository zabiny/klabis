package com.klabis.common.ui;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.users.Authority;
import com.klabis.common.users.infrastructure.restapi.RootApi;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API root resource for HAL+JSON navigation.
 */
@RestController
@RequestMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
class RootController implements RootApi {

    // Links are added in postprocessors from respective modules, bound on EntityModel<RootModel>.
    @Override
    public ResponseEntity<RootModel> rootNavigation() {
        // RootModel is an empty marker with no domain object of its own, but HalResponseBodyAdvice
        // only wraps a plain payload when the context holds a non-null domain — so a placeholder
        // stands in for one. It is never serialized (EntityModelWithDomain.domainItem is @JsonIgnore).
        HalResponseContext.setDomain(ROOT_DOMAIN_PLACEHOLDER);
        return ResponseEntity.ok(new RootModel());
    }

    private static final String ROOT_DOMAIN_PLACEHOLDER = "root";

}

/**
 * Adds the {@code admin} link that unlocks developer-only UI affordances (raw API response
 * inspection, admin route-limiting mode). Gated on the {@link Authority#DEVELOPER} authority,
 * which the bootstrap admin holds implicitly.
 */
@MvcComponent
class RootAdminLinkProcessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof KlabisJwtAuthenticationToken klabisAuth
            && klabisAuth.isAuthenticated()
            && klabisAuth.hasAuthority(Authority.DEVELOPER)) {
            model.add(Link.of("/sandplace").withRel("admin"));
        }
        return model;
    }
}
