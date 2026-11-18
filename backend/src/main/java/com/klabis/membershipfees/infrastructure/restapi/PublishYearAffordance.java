package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.common.ui.HalFormsOptionsDef;
import com.klabis.common.users.Authority;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.MembershipFeeTypeManagementPort;
import org.springframework.hateoas.Affordance;

import java.util.List;
import java.util.Map;

import static com.klabis.common.ui.HalFormsSupport.klabisAffordWithOptions;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Single source of the publishYear template, shared by every resource that offers it, so that the
 * "only one active campaign" rule and the type options are not duplicated.
 */
@MvcComponent
class PublishYearAffordance {

    private final MembershipFeeTypeManagementPort typeManagementPort;
    private final FeeSelectionCampaignManagementPort campaignManagementPort;
    private final AuthorizationEvaluator authorizationEvaluator;

    PublishYearAffordance(MembershipFeeTypeManagementPort typeManagementPort,
                          FeeSelectionCampaignManagementPort campaignManagementPort,
                          AuthorizationEvaluator authorizationEvaluator) {
        this.typeManagementPort = typeManagementPort;
        this.campaignManagementPort = campaignManagementPort;
        this.authorizationEvaluator = authorizationEvaluator;
    }

    List<Affordance> affordances() {
        if (!authorizationEvaluator.has(Authority.MEMBERS_MANAGE)) {
            return List.of();
        }
        return affordances(campaignManagementPort.findActiveCampaign().isPresent());
    }

    /** For callers that already looked up the active campaign. The caller must have checked MEMBERS_MANAGE. */
    List<Affordance> affordances(boolean campaignActive) {
        if (campaignActive) {
            return List.of();
        }
        List<HalFormsInlineOption> typeOptions = typeManagementPort.listFeeTypes().stream()
                .map(type -> new HalFormsInlineOption(type.getId().value().toString(), type.getName()))
                .toList();
        return klabisAffordWithOptions(
                methodOn(FeeSelectionCampaignsApi.class).publishYear(null),
                Map.of("typeIds", new HalFormsOptionsDef.Inline(typeOptions)));
    }
}
