package com.klabis.calendar.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.members.MemberResource;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Adds the {@code ical-token} link (with regenerate affordance) to the member detail resource,
 * but only when the authenticated user is viewing their own profile.
 * <p>
 * The iCal feed token is strictly personal — only its owner may manage it.
 * Admins and other members viewing someone else's profile must not see this link.
 */
@MvcComponent
public class IcalTokenMemberDetailLinkProcessor implements RepresentationModelProcessor<EntityModel<MemberResource>> {

    @Override
    public EntityModel<MemberResource> process(EntityModel<MemberResource> model) {
        if (!isSelfDetail(model)) {
            return model;
        }
        klabisLinkTo(methodOn(IcalTokenController.class).getTokenState(null))
                .ifPresent(link -> model.add(
                        link.withRel("ical-token")
                                .andAffordances(klabisAfford(methodOn(IcalTokenController.class).generateToken(null)))
                ));
        return model;
    }

    private boolean isSelfDetail(EntityModel<MemberResource> model) {
        MemberResource content = model.getContent();
        if (content == null || content.memberId() == null) {
            return false;
        }
        UUID resourceMemberId = content.memberId().uuid();
        if (resourceMemberId == null) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof KlabisJwtAuthenticationToken token)) {
            return false;
        }
        return token.getMemberIdUuid()
                .map(resourceMemberId::equals)
                .orElse(false);
    }
}
