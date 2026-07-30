package com.klabis.common.ui;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.infrastructure.restapi.RootApi;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Root", description = "API root navigation")
@SecurityRequirement(name = "KlabisAuth", scopes = {"openid"})
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
 * Carries over the admin link the controller used to add inline, now that it returns a plain
 * payload and no longer owns an EntityModel to attach links to.
 */
@MvcComponent
class RootAdminLinkProcessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
            && "admin".equalsIgnoreCase(authentication.getName())) {
            model.add(Link.of("/sandplace").withRel("admin"));
        }
        return model;
    }
}
