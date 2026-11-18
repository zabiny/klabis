package com.klabis.members.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.users.infrastructure.restapi.MyProfileApi;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Signals to the client that the member detail belongs to the caller, who may change their own password.
 * Password change always acts on the caller's account, so other members' details must not offer it.
 */
@MvcComponent
class MemberChangePasswordLinkProcessor implements RepresentationModelProcessor<EntityModel<MemberDetailsResponse>> {

    @Override
    public EntityModel<MemberDetailsResponse> process(EntityModel<MemberDetailsResponse> model) {
        if (!isOwnDetail(model.getContent())) {
            return model;
        }
        klabisLinkTo(methodOn(MyProfileApi.class).changePassword(null, null))
                .ifPresent(link -> model.add(
                        link.withRel("changePassword")
                                .andAffordances(klabisAfford(methodOn(MyProfileApi.class).changePassword(null, null)))
                ));
        return model;
    }

    private boolean isOwnDetail(MemberDetailsResponse content) {
        if (content == null || content.id() == null) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof KlabisJwtAuthenticationToken token)) {
            return false;
        }
        return token.isSelf(content.id());
    }
}
