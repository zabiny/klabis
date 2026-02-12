package com.klabis.common.ui;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API root resource for HAL+JSON navigation.
 */
@RestController
@RequestMapping(value = "/api", produces = {MediaTypes.HAL_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
@Tag(name = "Root", description = "API root navigation")
class RootController {

    // API for HAL ROOT endpoint. Main purpose is to have root node where HAL viewer can start viewing Klabis API. Links are added in postprocessors from respective modules.
    @GetMapping
    public EntityModel<RootModel> rootNavigation(Authentication authentication) {
        EntityModel<RootModel> result = EntityModel.of(new RootModel());
        if (authentication != null && authentication.isAuthenticated() && "admin".equalsIgnoreCase(authentication.getName())) {
            result.add(Link.of("/sandplace").withRel("admin"));
        }
        return result;
    }

}
