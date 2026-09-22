package com.klabis.members.infrastructure.orissync;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.RootModel;
import com.klabis.oris.OrisClubKeyApi;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * The club key is a club-wide administrative setting reached from the API root rather than from
 * any member (design.md D10) — mirrors {@code RootAdminLinkProcessor}. Only visible to a user
 * holding SYNC:MANAGE, via {@code klabisLinkTo}'s authorization check on {@code getClubKeyState}.
 */
@MvcComponent
public class OrisClubKeyRootLinkProcessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(OrisClubKeyApi.class).getClubKeyState())
                .ifPresent(link -> model.add(link.withRel("orisClubKey")));
        return model;
    }
}
