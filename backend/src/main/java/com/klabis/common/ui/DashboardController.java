package com.klabis.common.ui;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.infrastructure.restapi.DashboardApi;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
@Tag(name = "Dashboard", description = "Dashboard widget link index")
@SecurityRequirement(name = "KlabisAuth", scopes = {"openid"})
class DashboardController implements DashboardApi {

    @Override
    public ResponseEntity<DashboardModel> dashboard() {
        // DashboardModel is an empty marker with no domain object of its own, but
        // HalResponseBodyAdvice only wraps a plain payload when the context holds a non-null domain
        // — so a placeholder stands in for one. It is never serialized
        // (EntityModelWithDomain.domainItem is @JsonIgnore).
        HalResponseContext.setDomain(DASHBOARD_DOMAIN_PLACEHOLDER);
        return ResponseEntity.ok(new DashboardModel());
    }

    private static final String DASHBOARD_DOMAIN_PLACEHOLDER = "dashboard";

}

/**
 * Carries over the self link the controller used to add inline, now that it returns a plain payload.
 * wrapSingle() in HalResponseBodyAdvice adds a self link only for collection responses, not for
 * single ones, so without this the dashboard would answer with no _links at all.
 */
@MvcComponent
class DashboardSelfLinkProcessor implements RepresentationModelProcessor<EntityModel<DashboardModel>> {

    @Override
    public EntityModel<DashboardModel> process(EntityModel<DashboardModel> model) {
        klabisLinkTo(methodOn(DashboardApi.class).dashboard())
                .ifPresent(link -> model.add(link.withSelfRel()));
        return model;
    }
}
