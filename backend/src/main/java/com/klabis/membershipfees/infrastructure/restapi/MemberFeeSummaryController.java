package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.users.Authority;
import com.klabis.members.ActingMember;
import com.klabis.members.MemberId;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.MemberFeeHistoryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAffordWithPromptedOptions;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "MemberFeeSummary", description = "Member fee level summary and history API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.MEMBERS_SCOPE})
// Owner-only authorization, declared as x-klabis-owner-visible with no paired authority — see
// MemberFeeChoiceController for why actingMember stays despite being unused here.
class MemberFeeSummaryController implements MemberFeeSummaryApi {

    private final MemberFeeHistoryPort memberFeeHistoryPort;
    private final FeeSelectionCampaignManagementPort publicationManagementPort;

    MemberFeeSummaryController(MemberFeeHistoryPort memberFeeHistoryPort,
                                FeeSelectionCampaignManagementPort publicationManagementPort) {
        this.memberFeeHistoryPort = memberFeeHistoryPort;
        this.publicationManagementPort = publicationManagementPort;
    }

    @Override
    @Operation(summary = "Get member's current fee level summary for a year")
    public ResponseEntity<MemberFeeSummaryResponse> getFeeSummary(
            @Parameter(description = "Member UUID") UUID memberId,
            @Parameter(description = "Calendar year") Integer year,
            @ActingMember MemberId actingMember) {

        MemberFeeHistoryPort.CurrentLevelInfo info = memberFeeHistoryPort.getCurrentLevelInfo(
                new MemberId(memberId), year);
        MemberFeeSummaryResponse response = MemberFeeSummaryResponse.from(info);

        List<HalFormsInlineOption> groupOptions = publicationManagementPort.listGroupsForYear(year).stream()
                .map(group -> new HalFormsInlineOption(group.getId().value().toString(), group.getName()))
                .toList();

        HalResponseContext.setDomain(new FeeSummaryView(memberId, year, info, groupOptions));
        return ResponseEntity.ok(response);
    }

    // groupOptions is computed here (the controller already holds publicationManagementPort)
    // rather than in the postprocessor — @MvcComponent beans are discovered by @WebMvcTest's
    // global component scan regardless of the controllers under test (see backend-patterns
    // skill), so injecting the port there would break every unrelated @WebMvcTest slice in the
    // app unless each one also mocked it.
    record FeeSummaryView(UUID memberId, int year, MemberFeeHistoryPort.CurrentLevelInfo info,
                          List<HalFormsInlineOption> groupOptions) {
    }

    @Override
    @Operation(summary = "Get member's fee level assignment history")
    public ResponseEntity<MemberFeeHistoryResponse> getFeeHistory(
            @Parameter(description = "Member UUID") UUID memberId,
            @ActingMember MemberId actingMember) {

        MemberFeeHistoryResponse response = MemberFeeHistoryResponse.from(
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

    @Override
    public void process(EntityModel<MemberFeeSummaryResponse> dtoModel, MemberFeeSummaryController.FeeSummaryView view) {
        UUID memberId = view.memberId();
        int year = view.year();
        MemberFeeHistoryPort.CurrentLevelInfo info = view.info();
        List<HalFormsInlineOption> groupOptions = view.groupOptions();

        klabisLinkTo(methodOn(MemberFeeSummaryApi.class).getFeeSummary(memberId, year, null))
                .ifPresent(link -> {
                    var self = link.withSelfRel();
                    if (info.votingOpen()) {
                        self = self.andAffordances(klabisAffordWithPromptedOptions(
                                methodOn(MemberFeeChoiceApi.class).chooseTier(memberId, year, null, null),
                                Map.of("membershipFeeGroupId", groupOptions)));
                    }
                    dtoModel.add(self);
                });

        if (info.groupId() != null) {
            klabisLinkTo(methodOn(MembershipFeeGroupsApi.class).getFeeGroup(info.groupId().value()))
                    .ifPresent(link -> dtoModel.add(link.withRel("group")));
        }
    }
}
