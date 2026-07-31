package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.members.ActingMember;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.application.AdminFeeAssignmentPort;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.domain.FeeGroupMembership;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.PublishedLevelStatus;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.klabis.common.ui.HalFormsSupport.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@ExposesResourceFor(MembershipFeeGroup.class)
class MembershipFeeGroupController implements MembershipFeeGroupsApi {

    private final FeeSelectionCampaignManagementPort managementPort;
    private final AdminFeeAssignmentPort adminFeeAssignmentPort;
    private final Members members;

    MembershipFeeGroupController(FeeSelectionCampaignManagementPort managementPort,
                                 AdminFeeAssignmentPort adminFeeAssignmentPort,
                                 Members members) {
        this.managementPort = managementPort;
        this.adminFeeAssignmentPort = adminFeeAssignmentPort;
        this.members = members;
    }

    @Override
    public ResponseEntity<MembershipFeeGroupResponse> getFeeGroup(
            UUID id) {
        MembershipFeeGroup group = managementPort.getGroup(new MembershipFeeGroupId(id));

        // The members collection is declared here, not in the postprocessor: it needs the Members
        // port, and MembershipFeeGroupDetailsPostprocessor is shared with
        // FeeSelectionCampaignController.listGroupsForYear, whose items carry no such collection.
        HalResponseContext.setDomain(group);
        HalResponseContext.embed(buildGroupMembers(group),
                MembershipFeeGroupResponse.MemberInGroupResponse.class);
        return ResponseEntity.ok(MembershipFeeGroupResponse.from(group));
    }

    private List<MembershipFeeGroupResponse.MemberInGroupResponse> buildGroupMembers(MembershipFeeGroup group) {
        Set<FeeGroupMembership> memberships = group.getMemberships();
        if (memberships.isEmpty()) {
            return List.of();
        }
        Set<MemberId> memberIds = memberships.stream().map(FeeGroupMembership::memberId).collect(Collectors.toSet());
        Map<MemberId, MemberDto> memberDtos = members.findByIds(memberIds);
        return memberships.stream()
                .map(membership -> MembershipFeeGroupResponse.MemberInGroupResponse.from(membership, memberDtos.get(membership.memberId())))
                .toList();
    }

    @Override
    public ResponseEntity<Void> editSnapshot(
            UUID id,
            EditGroupSnapshotRequest request) {
        managementPort.editGroupSnapshot(new MembershipFeeGroupId(id), MembershipFeesRequestMapper.toCommand(request));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<java.util.Collection<MembershipFeeTierResponse.PaymentRuleResponse>> listGroupRules(
            UUID id) {
        MembershipFeeGroup group = managementPort.getGroup(new MembershipFeeGroupId(id));
        List<MembershipFeeTierResponse.PaymentRuleResponse> items = group.getRulesSnapshot().stream()
                .map(MembershipFeeTierResponse.PaymentRuleResponse::from)
                .toList();
        return ResponseEntity.ok(items);
    }

    @Override
    public ResponseEntity<Void> assignMember(
            UUID groupId,
            AdminAssignMemberRequest request,
            @ActingMember MemberId actingAdmin) {

        adminFeeAssignmentPort.assignLevel(new AdminFeeAssignmentPort.AssignFeeLevel(
                actingAdmin,
                new MemberId(request.memberId()),
                new MembershipFeeGroupId(groupId),
                request.year()));

        return ResponseEntity.noContent().build();
    }
}

@MvcComponent
class MembershipFeeGroupDetailsPostprocessor
        extends ModelWithDomainPostprocessor<MembershipFeeGroupResponse, MembershipFeeGroup> {

    @Override
    public void process(EntityModel<MembershipFeeGroupResponse> dtoModel, MembershipFeeGroup group) {
        UUID id = group.getId().value();
        klabisLinkTo(methodOn(MembershipFeeGroupsApi.class).getFeeGroup(id))
                .map(link -> {
                    var self = link.withSelfRel()
                            .andAffordances(klabisAfford(
                                    methodOn(MembershipFeeGroupsApi.class).assignMember(id, null, null)));
                    if (group.getStatus() == PublishedLevelStatus.EDITABLE) {
                        self = self.andAffordances(klabisAfford(
                                methodOn(MembershipFeeGroupsApi.class).editSnapshot(id, null)));
                    }
                    return self;
                })
                .ifPresent(dtoModel::add);
        klabisLinkTo(methodOn(MembershipFeeGroupsApi.class).listGroupRules(id))
                .ifPresent(link -> dtoModel.add(link.withRel("rules")));
        klabisLinkTo(methodOn(MembershipFeeTiersApi.class).getTier(group.getSourceLevelId().value()))
                .ifPresent(link -> dtoModel.add(link.withRel("sourceLevel")));
    }

}
