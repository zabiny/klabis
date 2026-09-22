package com.klabis.members.infrastructure.orissync;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.oris.ClubKeyStateResponse;
import com.klabis.oris.OrisClubKeyApi;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Adds exactly one of the 'set'/'clear' affordances depending on whether a key is currently held
 * (design.md D10) — the state itself is the affordance, so a client never has to decide which
 * action applies.
 */
@MvcComponent
class OrisClubKeyResponsePostprocessor implements RepresentationModelProcessor<EntityModel<ClubKeyStateResponse>> {

    @Override
    public EntityModel<ClubKeyStateResponse> process(EntityModel<ClubKeyStateResponse> model) {
        boolean isSet = model.getContent().isSet();

        klabisLinkTo(methodOn(OrisClubKeyApi.class).getClubKeyState()).map(link -> {
            Link self = link.withSelfRel();
            if (isSet) {
                self = self.andAffordances(klabisAfford(methodOn(OrisClubKeyApi.class).clearClubKey()));
            } else {
                self = self.andAffordances(klabisAfford(methodOn(OrisClubKeyApi.class).setClubKey(null)));
            }
            return self;
        }).ifPresent(model::add);

        return model;
    }
}
