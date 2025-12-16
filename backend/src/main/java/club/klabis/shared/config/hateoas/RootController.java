package club.klabis.shared.config.hateoas;

import club.klabis.shared.config.restapi.ApiController;
import club.klabis.shared.config.restapi.KlabisPrincipal;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;

@ApiController(path = "/", openApiTagName = "Misc")
public class RootController {

    // API for HAL ROOT endpoint. Main purpose is to have root node where HAL viewer can start viewing Klabis API. Links are added in postprocessors from respective modules.
    @GetMapping(path = {"/", "/api"}, produces = MediaTypes.HAL_JSON_VALUE)
    public EntityModel<RootModel> rootNavigation(@AuthenticationPrincipal KlabisPrincipal user) {
        return EntityModel.of(new RootModel(user));
    }

}
