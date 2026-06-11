package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.common.users.Authority;
import com.klabis.members.ActingMember;
import com.klabis.members.MemberId;
import com.klabis.membershipfees.application.FeeYearPublicationManagementPort;
import com.klabis.membershipfees.application.MemberFeeHistoryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping(value = "/api/members/{memberId}", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "MemberFeeSummary", description = "Member fee level summary and history API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.MEMBERS_SCOPE})
class MemberFeeSummaryController {

    private final MemberFeeHistoryPort memberFeeHistoryPort;
    private final FeeYearPublicationManagementPort publicationManagementPort;

    MemberFeeSummaryController(MemberFeeHistoryPort memberFeeHistoryPort,
                                FeeYearPublicationManagementPort publicationManagementPort) {
        this.memberFeeHistoryPort = memberFeeHistoryPort;
        this.publicationManagementPort = publicationManagementPort;
    }

    @GetMapping("/fee-summary/{year}")
    @Operation(summary = "Get member's current fee level summary for a year")
    ResponseEntity<EntityModel<MemberFeeSummaryResponse>> getFeeSummary(
            @Parameter(description = "Member UUID") @PathVariable UUID memberId,
            @Parameter(description = "Calendar year") @PathVariable int year,
            @ActingMember MemberId actingMember) {

        assertMemberAccessingSelf(memberId, actingMember);

        MemberFeeHistoryPort.CurrentLevelInfo info = memberFeeHistoryPort.getCurrentLevelInfo(
                new MemberId(memberId), year);
        MemberFeeSummaryResponse response = MemberFeeSummaryResponse.from(info);
        EntityModel<MemberFeeSummaryResponse> model = EntityModel.of(response);

        List<HalFormsInlineOption> groupOptions = publicationManagementPort.listGroupsForYear(year).stream()
                .map(group -> new HalFormsInlineOption(group.getId().value().toString(), group.getName()))
                .toList();

        klabisLinkTo(methodOn(MemberFeeSummaryController.class).getFeeSummary(memberId, year, null))
                .ifPresent(link -> {
                    var self = link.withSelfRel();
                    if (info.votingOpen()) {
                        self = self.andAffordances(klabisAffordWithPromptedOptions(
                                methodOn(MemberFeeChoiceController.class).chooseTier(memberId, year, null, null),
                                Map.of("membershipFeeGroupId", groupOptions)));
                    }
                    model.add(self);
                });

        if (info.groupId() != null) {
            klabisLinkTo(methodOn(MembershipFeeGroupController.class).getGroup(info.groupId().value()))
                    .ifPresent(link -> model.add(link.withRel("group")));
        }

        return ResponseEntity.ok(model);
    }

    @GetMapping("/fee-history")
    @Operation(summary = "Get member's fee level assignment history")
    ResponseEntity<EntityModel<MemberFeeHistoryResponse>> getFeeHistory(
            @Parameter(description = "Member UUID") @PathVariable UUID memberId,
            @ActingMember MemberId actingMember) {

        assertMemberAccessingSelf(memberId, actingMember);

        MemberFeeHistoryResponse response = MemberFeeHistoryResponse.from(
                memberFeeHistoryPort.getLevelHistory(new MemberId(memberId)));
        EntityModel<MemberFeeHistoryResponse> model = EntityModel.of(response);

        klabisLinkTo(methodOn(MemberFeeSummaryController.class).getFeeHistory(memberId, null))
                .ifPresent(link -> model.add(link.withSelfRel()));

        return ResponseEntity.ok(model);
    }

    private void assertMemberAccessingSelf(UUID memberId, MemberId actingMember) {
        if (!actingMember.value().equals(memberId)) {
            throw new AccessDeniedException("Members can only access their own fee summary and history");
        }
    }
}
