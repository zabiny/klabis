package com.klabis.members.legalguardian.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.ui.RootModel;
import com.klabis.members.LegalGuardians;
import com.klabis.members.infrastructure.restapi.LegalGuardiansApi;
import com.klabis.members.infrastructure.restapi.MembersApi;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * The frontend opens "my profile" from this link instead of assuming every user is a member.
 */
@MvcComponent
class RootProfileLinkProcessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    private final LegalGuardians legalGuardians;

    RootProfileLinkProcessor(LegalGuardians legalGuardians) {
        this.legalGuardians = legalGuardians;
    }

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof KlabisJwtAuthenticationToken token) || !token.isAuthenticated()) {
            return model;
        }

        if (token.hasMemberProfile()) {
            klabisLinkTo(methodOn(MembersApi.class).getMember(token.getMemberIdUuid().orElseThrow(), null))
                    .ifPresent(link -> model.add(link.withRel("profile")));
        } else if (legalGuardians.findById(token.getUserId()).isPresent()) {
            klabisLinkTo(methodOn(LegalGuardiansApi.class).getLegalGuardian(token.getUserId().uuid()))
                    .ifPresent(link -> model.add(link.withRel("profile")));
        }
        return model;
    }
}
