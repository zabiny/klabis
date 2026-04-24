package com.klabis.common.ui;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/api/dashboard", produces = {MediaTypes.HAL_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
@Tag(name = "Dashboard", description = "Dashboard widget link index")
@SecurityRequirement(name = "KlabisAuth", scopes = {"openid"})
class DashboardController {

    @GetMapping
    public EntityModel<DashboardModel> dashboard() {
        EntityModel<DashboardModel> result = EntityModel.of(new DashboardModel());
        klabisLinkTo(methodOn(DashboardController.class).dashboard())
                .ifPresent(link -> result.add(link.withSelfRel()));
        return result;
    }

}
