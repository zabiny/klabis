package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.common.ui.HalFormsOptionsDef;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.members.ActingMember;
import com.klabis.members.MemberId;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.MemberFeeHistoryPort;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAffordWithOptions;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
// Owner-only authorization, declared as x-klabis-owner-visible with no paired authority — see
// MemberFeeChoiceController for why actingMember stays despite being unused here.
class MemberFeeSummaryController implements MemberFeeSummaryApi {

    private final MemberFeeHistoryPort memberFeeHistoryPort;

    MemberFeeSummaryController(MemberFeeHistoryPort memberFeeHistoryPort) {
        this.memberFeeHistoryPort = memberFeeHistoryPort;
    }

    @Override
    public ResponseEntity<MemberFeeSummaryResponse> getFeeSummary(
            UUID memberId,
            Integer year,
            @ActingMember MemberId actingMember) {

        MemberFeeHistoryPort.CurrentLevelInfo info = memberFeeHistoryPort.getCurrentLevelInfo(
                new MemberId(memberId), year);
        MemberFeeSummaryResponse response = MembershipFeesResponseMapper.toResponse(info);

        HalResponseContext.setDomain(new FeeSummaryView(memberId, year, info));
        return ResponseEntity.ok(response);
    }

    record FeeSummaryView(UUID memberId, int year, MemberFeeHistoryPort.CurrentLevelInfo info) {
    }

    @Override
    public ResponseEntity<MemberFeeHistoryResponse> getFeeHistory(
            UUID memberId,
            @ActingMember MemberId actingMember) {

        MemberFeeHistoryResponse response = MembershipFeesResponseMapper.toResponse(
                memberFeeHistoryPort.getLevelHistory(new MemberId(memberId)));

        HalResponseContext.setDomain(memberId);
        return ResponseEntity.ok(response);
    }
}

@MvcComponent
class MemberFeeHistoryDetailsPostprocessor extends ModelWithDomainPostprocessor<MemberFeeHistoryResponse, UUID> {

    @Override
    public void process(EntityModel<MemberFeeHistoryResponse> dtoModel, UUID memberId) {
        klabisLinkTo(methodOn(MemberFeeSummaryApi.class).getFeeHistory(memberId, null))
                .ifPresent(link -> dtoModel.add(link.withSelfRel()));
    }
}

@MvcComponent
class MemberFeeSummaryDetailsPostprocessor
        extends ModelWithDomainPostprocessor<MemberFeeSummaryResponse, MemberFeeSummaryController.FeeSummaryView> {

    private final FeeSelectionCampaignManagementPort publicationManagementPort;

    MemberFeeSummaryDetailsPostprocessor(FeeSelectionCampaignManagementPort publicationManagementPort) {
        this.publicationManagementPort = publicationManagementPort;
    }

    @Override
    public void process(EntityModel<MemberFeeSummaryResponse> dtoModel, MemberFeeSummaryController.FeeSummaryView view) {
        UUID memberId = view.memberId();
        int year = view.year();
        MemberFeeHistoryPort.CurrentLevelInfo info = view.info();

        klabisLinkTo(methodOn(MemberFeeSummaryApi.class).getFeeSummary(memberId, year, null))
                .ifPresent(link -> {
                    var self = link.withSelfRel();
                    if (info.votingOpen()) {
                        List<HalFormsInlineOption> groupOptions = publicationManagementPort.listGroupsForYear(year).stream()
                                .map(group -> new HalFormsInlineOption(group.getId().value().toString(), group.getName()))
                                .toList();
                        self = self.andAffordances(klabisAffordWithOptions(
                                methodOn(MemberFeeChoiceApi.class).chooseTier(memberId, year, null, null),
                                Map.of("membershipFeeGroupId", new HalFormsOptionsDef.Inline(groupOptions))));
                    }
                    dtoModel.add(self);
                });

        if (info.groupId() != null) {
            klabisLinkTo(methodOn(MembershipFeeGroupsApi.class).getFeeGroup(info.groupId().value()))
                    .ifPresent(link -> dtoModel.add(link.withRel("group")));
        }
    }
}
