package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.members.MemberResource;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Adds a {@code feeSummary} link to the member detail resource pointing to the fee summary
 * for the most relevant year — the active campaign year when one exists, or the current year.
 * <p>
 * Only added when the authenticated user is viewing their own profile, because
 * {@code getFeeSummary} enforces self-access and would return 403 for others.
 */
@MvcComponent
public class MemberFeeSummaryLinkProcessor implements RepresentationModelProcessor<EntityModel<MemberResource>> {

    private final Optional<FeeSelectionCampaignManagementPort> campaignManagementPort;
    private final Clock clock;

    MemberFeeSummaryLinkProcessor(Optional<FeeSelectionCampaignManagementPort> campaignManagementPort, Clock clock) {
        this.campaignManagementPort = campaignManagementPort;
        this.clock = clock;
    }

    @Override
    public EntityModel<MemberResource> process(EntityModel<MemberResource> model) {
        if (campaignManagementPort.isEmpty()) {
            return model;
        }

        MemberResource content = model.getContent();
        if (content == null || content.memberId() == null) {
            return model;
        }

        UUID resourceMemberId = content.memberId().uuid();
        if (resourceMemberId == null) {
            return model;
        }

        if (!isViewingOwnProfile(resourceMemberId)) {
            return model;
        }

        int year = campaignManagementPort.get().relevantFeeYear(LocalDate.now(clock));
        klabisLinkTo(methodOn(MemberFeeSummaryController.class).getFeeSummary(resourceMemberId, year, null))
                .ifPresent(link -> model.add(link.withRel("feeSummary")));

        return model;
    }

    private boolean isViewingOwnProfile(UUID resourceMemberId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof KlabisJwtAuthenticationToken token)) {
            return false;
        }
        return token.getMemberIdUuid()
                .map(resourceMemberId::equals)
                .orElse(false);
    }
}
